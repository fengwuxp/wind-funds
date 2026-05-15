package com.capte.funds.ledger;

import com.wind.integration.funds.ledger.CompositeLedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeLedgerPostingAssemblerTests {

    @Test
    void supportsAndAssembleShouldSkipCompositeItselfFromDelegates() {
        AtomicInteger supportCalls = new AtomicInteger();
        AtomicInteger assembleCalls = new AtomicInteger();
        List<LedgerPostingAssembler<? extends ResolvedRouteSpec>> delegates = new ArrayList<>();
        CompositeLedgerPostingAssembler composite = new CompositeLedgerPostingAssembler(delegates);
        delegates.add(composite);
        delegates.add(new LedgerPostingAssembler<>() {
            @Override
            public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                           @NonNull String fundsTransactionSn,
                                                           @NonNull ResolvedRouteSpec resolvedRoute) {
                assembleCalls.incrementAndGet();
                return transaction();
            }

            @Override
            public boolean supports(@NonNull ResolvedRouteSpec resolvedRoute) {
                supportCalls.incrementAndGet();
                return true;
            }
        });

        ResolvedRouteSpec route = route();

        assertThat(composite.supports(route)).isTrue();
        assertThat(composite.assemble(instruction(), "FT_0001", route)).isSameAs(transaction());
        assertThat(supportCalls.get()).isEqualTo(2);
        assertThat(assembleCalls.get()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private static FundsInstructionSpec instruction() {
        return (FundsInstructionSpec) Proxy.newProxyInstance(
                FundsInstructionSpec.class.getClassLoader(),
                new Class<?>[]{FundsInstructionSpec.class},
                (proxy, method, args) -> null
        );
    }

    @SuppressWarnings("unchecked")
    private static LedgerTransactionSpec transaction() {
        return Holder.TRANSACTION;
    }

    private static final class Holder {
        private static final LedgerTransactionSpec TRANSACTION = (LedgerTransactionSpec) Proxy.newProxyInstance(
                LedgerTransactionSpec.class.getClassLoader(),
                new Class<?>[]{LedgerTransactionSpec.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "LedgerTransactionProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    return null;
                }
        );
    }

    @SuppressWarnings("unchecked")
    private static ResolvedRouteSpec route() {
        return (ResolvedRouteSpec) Proxy.newProxyInstance(
                ResolvedRouteSpec.class.getClassLoader(),
                new Class<?>[]{ResolvedRouteSpec.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "ResolvedRouteProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    if (List.class.isAssignableFrom(method.getReturnType())) {
                        return List.of();
                    }
                    return null;
                }
        );
    }
}
