package com.capte.funds.transaction.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.transaction.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.transaction.services.SubjectLedgerInitializer;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

final class FundsAccountServiceTestSupport {

    private FundsAccountServiceTestSupport() {
    }

    @SuppressWarnings("unchecked")
    static <T> T mapper(Class<T> mapperType,
                        Consumer<Object> insertSelectiveHandler,
                        Function<QueryWrapper, Object> selectOneByQueryHandler) {
        return mapper(mapperType, insertSelectiveHandler, selectOneByQueryHandler, entity -> {
            throw new UnsupportedOperationException("update");
        });
    }

    @SuppressWarnings("unchecked")
    static <T> T mapper(Class<T> mapperType,
                        Consumer<Object> insertSelectiveHandler,
                        Function<QueryWrapper, Object> selectOneByQueryHandler,
                        Function<Object, Integer> updateHandler) {
        return mapper(mapperType, insertSelectiveHandler, selectOneByQueryHandler, query -> {
            throw new UnsupportedOperationException("selectListByQuery");
        }, updateHandler);
    }

    @SuppressWarnings("unchecked")
    static <T> T mapper(Class<T> mapperType,
                        Consumer<Object> insertSelectiveHandler,
                        Function<QueryWrapper, Object> selectOneByQueryHandler,
                        Function<QueryWrapper, List<?>> selectListByQueryHandler,
                        Function<Object, Integer> updateHandler) {
        return (T) Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return invokeObjectMethod(proxy, method, args);
                    }
                    if ("insertSelective".equals(method.getName())) {
                        insertSelectiveHandler.accept(args[0]);
                        return 1;
                    }
                    if ("selectOneByQuery".equals(method.getName())) {
                        return selectOneByQueryHandler.apply((QueryWrapper) args[0]);
                    }
                    if ("selectListByQuery".equals(method.getName())) {
                        return selectListByQueryHandler.apply((QueryWrapper) args[0]);
                    }
                    if ("update".equals(method.getName())) {
                        return updateHandler.apply(args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    static LedgerService ledgerServiceWithCreateRecorder(List<CreateLedgerRequest> requests) {
        AtomicLong sequence = new AtomicLong(100L);
        return new UnsupportedLedgerService() {
            @Override
            public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
                requests.add(request);
                return sequence.incrementAndGet();
            }
        };
    }

    static LedgerService unsupportedLedgerService() {
        return new UnsupportedLedgerService();
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "Proxy(" + proxy.getClass().getInterfaces()[0].getSimpleName() + ")";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    static final class RecordingSubjectLedgerInitializer implements SubjectLedgerInitializer {

        private InitializeSubjectLedgerRequest request;

        @Override
        public @NonNull Map<LedgerSubjectCode, Long> initializeRequiredLedgers(
                @NonNull InitializeSubjectLedgerRequest request) {
            this.request = request;
            return Map.of();
        }

        InitializeSubjectLedgerRequest getRequest() {
            return request;
        }
    }

    private static class UnsupportedLedgerService implements LedgerService {

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException("createLedger");
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            throw new UnsupportedOperationException("updateLedgerBalance");
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException("deleteLedgerByIds");
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            throw new UnsupportedOperationException("getLedgerById");
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            throw new UnsupportedOperationException("getLedgerByIds");
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                               @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException("queryLedgers");
        }
    }
}
