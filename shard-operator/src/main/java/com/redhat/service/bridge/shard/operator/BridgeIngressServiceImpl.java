package com.redhat.service.bridge.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @ConfigProperty(name = "event-bridge.ingress.image")
    String ingressImage;

    @Override
    public void createBridgeIngress(BridgeDTO bridgeDTO) {
        String namespace = getOrCreateNamespace(bridgeDTO.getCustomerId());

        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace)
                .create(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
    }

    @Override
    public void deleteBridgeIngress(BridgeDTO bridgeDTO) {
        String namespace = customerNamespaceProvider.resolveNamespace(bridgeDTO.getCustomerId());
        kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(namespace)
                .delete(BridgeIngress.fromDTO(bridgeDTO, namespace, ingressImage));
    }

    // TODO: https://issues.redhat.com/browse/MGDOBR-92 manage namespaces in a different service to be injected here
    private String getOrCreateNamespace(String customerId) {
        String namespace = customerNamespaceProvider.resolveNamespace(customerId);
        if (kubernetesClient.namespaces().withName(namespace).get() == null) {
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build();
            kubernetesClient.namespaces().create(ns);
        }
        return namespace;
    }
}
