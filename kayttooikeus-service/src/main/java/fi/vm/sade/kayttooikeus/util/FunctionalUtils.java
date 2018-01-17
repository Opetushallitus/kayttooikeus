package fi.vm.sade.kayttooikeus.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

public class FunctionalUtils {
    private FunctionalUtils() {
    }

    public static <E, T extends Collection<E>> BinaryOperator<T> appending() {
        return (t, u) -> {
            t.addAll(u);
            return t;
        };
    }

    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<T> consumer, Runnable runnable) {
        if (optional.isPresent()) {
            consumer.accept(optional.get());
        } else {
            runnable.run();
        }
    }

    public static <F,T> Function<F,T> or(Function<F,T> a, Function<F,T> b) {
        return f -> {
            T t = a.apply(f);
            return t != null ? t : b.apply(f);
        };
    }

    @FunctionalInterface
    public interface IOThrowingSupplier<T> {
        T get() throws IOException;
    }
    @FunctionalInterface
    public interface IOThrowingFunction<F,T> {
        T apply(F f) throws IOException;
    }
    
    public static class RuntimeIOExceptionWrapper extends RuntimeException {
        public RuntimeIOExceptionWrapper(IOException e) {
            super(e);
        }
    }
    
    public static<T> Supplier<T> io(IOThrowingSupplier<T> in) {
        return () -> {
            try {
                return in.get();
            } catch (IOException e) {
                throw new RuntimeIOExceptionWrapper(e);
            }
        };
    }

    public static<F,T> Function<F,T> io(IOThrowingFunction<F,T> in) {
        return f -> {
            try {
                return in.apply(f);
            } catch (IOException e) {
                throw new RuntimeIOExceptionWrapper(e);
            }
        };
    }

    public static <F,T> Function<F,T> retrying(Function<F,T> target, Function<F,T> with) {
        return f -> {
            try {
                return target.apply(f);
            } catch (RuntimeException e) {
                try {
                    return with.apply(f);
                } catch (RuntimeException e2) {
                    throw e;
                }
            }
        };
    }

    public static <T> Supplier<FailureResult<T, RuntimeException>> retrying(Supplier<T> target, int times) {
        return () -> {
            RuntimeException failure;
            int i = 0;
            do try {
                return FailureResult.success(target.get());
            } catch (RuntimeException e) {
                failure = e;
            } while (i++ < times);
            return FailureResult.fail(failure);
        };
    }

    public static <F,T> Function<F, FailureResult<T, RuntimeException>> retrying(Function<F,T> target, int times) {
        return f -> {
            RuntimeException failure;
            int i = 0;
            do try {
                return FailureResult.success(target.apply(f));
            } catch (RuntimeException e) {
                failure = e;
            } while (i++ < times);
            return FailureResult.fail(failure);
        };
    }

    public static <T> Supplier<T> retrying(Supplier<T> target, Supplier<T> with) {
        return () -> {
            try {
                return target.get();
            } catch (RuntimeException e) {
                try {
                    return with.get();
                } catch (RuntimeException e2) {
                    throw e;
                }
            }
        };
    }

    public static class FailureResult<T, Ex extends RuntimeException> {
        private final Ex failure;
        private final T result;

        protected FailureResult(T result, Ex failure) {
            this.result = result;
            this.failure = failure;
        }

        public static<T, Ex extends RuntimeException> FailureResult<T, Ex> success(T result) {
            return new FailureResult<>(result, null);
        }

        public static<T, Ex extends RuntimeException> FailureResult<T, Ex> fail(Ex failure) {
            return new FailureResult<>(null, failure);
        }

        public Optional<T> optional() {
            return ofNullable(result);
        }

        public T orFail() throws Ex {
            if (this.failure != null) {
                throw failure;
            }
            return result;
        }

        public<Ex2 extends Throwable> T orFail(Function<Ex, Ex2> translateException) throws Ex2 {
            if (this.failure != null) {
                throw translateException.apply(this.failure);
            }
            return result;
        }

        public <U> U as(BiFunction<T, Ex, U> mapper) {
            return mapper.apply(result, failure);
        }
    }
}
