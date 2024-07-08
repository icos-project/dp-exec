package es.bsc.compss.util;

import es.bsc.compss.scheduler.types.Profile;
import es.bsc.compss.types.CoreElement;

import es.bsc.compss.types.resources.MethodResourceDescription;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;


public class OTelFacade {

    public static final int DEFAULT_PROMETHEUS_PORT = 19090;


    private enum CEGauge {

        TASK_COUNT("compss.execTasks", "Number of executed tasks"),
        PENDING_TASKS_COUNT("compss.pendTasks", "Number of pending tasks"),
        MIN_TIME("compss.minTime", "Minimum time to execute task"),
        AVG_TIME("compss.avgTime", "Average time to execute task"),
        MAX_TIME("compss.maxTime", "Maximum time to execute task"),;


        private final String name;
        private final String desc;


        CEGauge(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }


    private static OpenTelemetry openTelemetry;

    private static Meter meter;
    private static int cpuCount = 0;
    private static long[][] ceMeters = new long[][] {};

    static {
        Attributes serviceAttr = Attributes.of(ResourceAttributes.SERVICE_NAME, "PrometheusExporterExample");
        Resource serviceResource = Resource.create(serviceAttr);
        Resource resource = Resource.getDefault().merge(serviceResource);
        String portStr = System.getProperty("PROMETHEUS_PORT");
        int port = DEFAULT_PROMETHEUS_PORT;
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception e) {
                // Do nothing. Default value already loaded
            }
        }
        PrometheusHttpServer server = PrometheusHttpServer.builder().setPort(port).build();

        SdkMeterProvider provider =
            SdkMeterProvider.builder().setResource(resource).registerMetricReader(server).build();

        OTelFacade.openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(provider).buildAndRegisterGlobal();

        OTelFacade.meter = openTelemetry.meterBuilder("compss").setInstrumentationVersion("1.0.0").build();

        Attributes cuAttr = Attributes.of(AttributeKey.stringKey("property"), "computing_units");
        meter.gaugeBuilder("compss.node_info")
            .buildWithCallback((mesurement -> mesurement.record(OTelFacade.cpuCount, cuAttr)));

    }


    /**
     * Removes all the metrics previously configured.
     */
    public static void clearCoreElements() {
        ceMeters = new long[][] {};
    }

    /**
     * Sets up the number of CPU cores in the node.
     */
    public static void updateNodeFeatures(MethodResourceDescription description) {
        OTelFacade.cpuCount = description.getTotalCPUComputingUnits();
    }

    /**
     * Updates the core elements from which the runtime could handle tasks.
     *
     * @param newCount number of CE.
     */
    public static void updateCoreElements(int newCount) {
        int oldCount = ceMeters.length;
        // Gets or creates a named meter instance Meter meter =

        long[][] newMeters = new long[newCount][CEGauge.values().length];
        if (oldCount != 0) {
            System.arraycopy(ceMeters, 0, newMeters, 0, ceMeters.length);
        }
        ceMeters = newMeters;
        for (int coreId = oldCount; coreId < newCount; coreId++) {
            final int cId = coreId;
            CoreElement ce = CoreManager.getCore(coreId);
            for (CEGauge g : CEGauge.values()) {
                String signature = ce.getSignature();
                Attributes newAttributes = Attributes.of(AttributeKey.stringKey("CoreSignature"), signature);
                meter.gaugeBuilder(g.name).setDescription(g.desc).setUnit("1")
                    .buildWithCallback((mesurement -> mesurement.record(ceMeters[cId][g.ordinal()], newAttributes)));
            }
        }
    }

    /**
     * Registers the creation of a new task.
     *
     * @param coreId CE of the new task
     */
    public static void newTask(int coreId) {
        ceMeters[coreId][CEGauge.PENDING_TASKS_COUNT.ordinal()]++;
    }

    /**
     * Registers the starting of the execution of a task.
     *
     * @param coreId CE of the started task
     */
    public static void startTask(int coreId) {
        ceMeters[coreId][CEGauge.PENDING_TASKS_COUNT.ordinal()]--;
    }

    /**
     * Registers the end of a given task.
     *
     * @param coreId CE of the finished task
     * @param profile task profiling information
     */
    public static void finishedTask(int coreId, Profile profile) {
        ceMeters[coreId][CEGauge.TASK_COUNT.ordinal()] = profile.getExecutionCount();
        ceMeters[coreId][CEGauge.MIN_TIME.ordinal()] = profile.getMinExecutionTime();
        ceMeters[coreId][CEGauge.AVG_TIME.ordinal()] = profile.getAverageExecutionTime();
        ceMeters[coreId][CEGauge.MAX_TIME.ordinal()] = profile.getMaxExecutionTime();
    }

}