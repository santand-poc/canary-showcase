package pl.santander.featureflag.engine;

import org.springframework.expression.AccessException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.concurrent.*;

public class GuardEngine {
    private final ExpressionParser parser;
    private final NormalizeUtil normalizer;
    private final GuardFunctions functions;
    private final MethodResolver whitelistResolver;
    private final ExecutorService exec = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "guard-eval");
        t.setDaemon(true);
        return t;
    });

    public GuardEngine(ExpressionParser parser, NormalizeUtil normalizer,
                       GuardFunctions functions, MethodResolver whitelistResolver) {
        this.parser = parser;
        this.normalizer = normalizer;
        this.functions = functions;
        this.whitelistResolver = whitelistResolver;
    }

    public CompiledGuard compile(String rawExpr) {
        String expr = normalizer.normalize(rawExpr);
        Expression e = parser.parseExpression(expr);
        return new CompiledGuard(e);
    }

    public final class CompiledGuard {
        private final Expression expression;

        private CompiledGuard(Expression e) {
            this.expression = e;
        }

        public Result evaluate(long timeoutMs) {
            Callable<Result> task = () -> {
                StandardEvaluationContext ctx = new StandardEvaluationContext();
                ctx.setBeanResolver((c, n) -> {
                    throw new AccessException("beans disabled");
                });
                ctx.setTypeLocator(typeName -> {
                    throw new RuntimeException("types disabled");
                });
                ctx.setPropertyAccessors(List.of());
                ctx.setMethodResolvers(List.of(whitelistResolver));
                ctx.setRootObject(functions);
                Boolean ok = expression.getValue(ctx, Boolean.class);
                if (ok == null) return Result.waiting("null");
                return ok ? Result.pass() : Result.fail();
            };
            Future<Result> f = exec.submit(task);
            try {
                return f.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                f.cancel(true);
                return Result.waiting("timeout");
            } catch (Exception e) {
                return Result.waiting("eval_error:" + e.getClass().getSimpleName());
            }
        }
    }

    public static final class Result {
        public enum Status {PASS, FAIL, WAIT}

        private final Status status;
        private final String note;

        private Result(Status s, String n) {
            this.status = s;
            this.note = n;
        }

        public static Result pass() {
            return new Result(Status.PASS, "");
        }

        public static Result fail() {
            return new Result(Status.FAIL, "");
        }

        public static Result waiting(String n) {
            return new Result(Status.WAIT, n);
        }

        public Status status() {
            return status;
        }

        public String note() {
            return note;
        }

        public String toString() {
            return status + (note.isEmpty() ? "" : "(" + note + ")");
        }
    }
}
