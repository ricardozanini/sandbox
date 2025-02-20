package com.redhat.service.bridge.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactory;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.micrometer.core.instrument.MeterRegistry;

// TODO: This class has to be removed when we switch to ExecutorConfigProviderImpl
@ApplicationScoped
public class ExecutorsProviderMock implements ExecutorsProvider,
        ExecutorsK8SDeploymentManager {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();
    private static final TransformationEvaluatorFactory transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();

    private final Map<String, Set<Executor>> bridgeToProcessorMap = new HashMap<>();

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Inject
    MeterRegistry registry;

    @Override
    public Set<Executor> getExecutors() {
        throw new UnsupportedOperationException("Not implemented by this mock.");
    }

    @Override
    public Set<Executor> getExecutors(String bridgeId) {
        return bridgeToProcessorMap.get(bridgeId);
    }

    @Override
    public void deploy(ProcessorDTO processorDTO) {

        Executor executor = new Executor(processorDTO, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactory, registry);

        synchronized (bridgeToProcessorMap) {
            Set<Executor> executors = bridgeToProcessorMap.get(processorDTO.getBridge().getId());
            if (executors == null) {
                executors = new HashSet<>();
            }

            executors.add(executor);
            bridgeToProcessorMap.put(processorDTO.getBridge().getId(), executors);
        }
    }

    @Override
    public void undeploy(String bridgeId, String processorId) {
        synchronized (bridgeToProcessorMap) {
            Set<Executor> executors = bridgeToProcessorMap.get(bridgeId);
            executors
                    .stream()
                    .filter(x -> x.getProcessor().getId().equals(processorId))
                    .findFirst()
                    .ifPresent(executors::remove);
        }
    }
}
