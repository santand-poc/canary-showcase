package pl.santander.featureflag.engine;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Resolver metod dopuszczający wyłącznie nazwy z białej listy.
 * Jeśli metoda nie jest dozwolona albo delegate nie znajdzie dopasowania – zwraca null (fail-closed).
 */
public class WhitelistMethodResolver implements MethodResolver {

    private final Set<String> allowed;
    private final ReflectiveMethodResolver delegate = new ReflectiveMethodResolver();

    public WhitelistMethodResolver(Set<String> allowed) {
        this.allowed = allowed;
    }

    @Override
    public @Nullable MethodExecutor resolve(
            EvaluationContext context,
            Object targetObject,
            String name,
            List<TypeDescriptor> argumentTypes) throws AccessException {

        if (!allowed.contains(name)) {
            return null;
        }

        MethodExecutor exec = delegate.resolve(context, targetObject, name, argumentTypes);
        if (exec == null) {
            return null;
        }

        if (exec instanceof ReflectiveMethodExecutor rme) {
            Method m = rme.getMethod();
            if (!allowed.contains(m.getName())) {
                return null;
            }
        }

        return exec;
    }
}
