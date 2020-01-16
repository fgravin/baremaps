import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.HttpServiceWithRoutes;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.docs.DocServiceBuilder;
import com.linecorp.armeria.server.docs.DocServiceFilter;
import com.linecorp.armeria.server.grpc.GrpcService;
import io.gazetteer.routing.Routing.RoutingRequest;
import io.gazetteer.routing.RoutingServiceGrpc;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingServiceMain {

  private static final Logger logger = LoggerFactory.getLogger(RoutingServiceMain.class);

  public static void main(String[] args) {
    int httpPort = 9090;
    int httpsPort = 9443;
    String dataFile = "";
    String graphhopperDirectory = "";

    GraphHopper graphhopper = new GraphHopperOSM().forServer();
    graphhopper.setDataReaderFile(dataFile);
    graphhopper.setGraphHopperLocation(graphhopperDirectory);
    graphhopper.setEncodingManager(EncodingManager.create("car"));
    graphhopper.importOrLoad();

    RoutingRequest exampleRequest = RoutingRequest.newBuilder().build();
    HttpServiceWithRoutes grpcService =
        GrpcService.builder()
            .addService(new RoutingService(graphhopper))
            .addService(ProtoReflectionService.newInstance())
            .supportedSerializationFormats(GrpcSerializationFormats.values())
            .enableUnframedRequests(true)
            .build();
    Server server = Server.builder()
        .http(httpPort)
        .https(httpsPort)
        .tlsSelfSigned()
        .service(grpcService)
        .serviceUnder("/docs", new DocServiceBuilder()
            .exampleRequestForMethod(RoutingServiceGrpc.SERVICE_NAME, "Routing", exampleRequest)
            .exclude(DocServiceFilter.ofServiceName(ServerReflectionGrpc.SERVICE_NAME))
            .build())
        .build();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      server.stop().join();
      logger.info("Server has been stopped.");
    }));

    server.start().join();
    InetSocketAddress localAddress = server.activePort().get().localAddress();
    boolean isLocalAddress = localAddress.getAddress().isAnyLocalAddress() ||
        localAddress.getAddress().isLoopbackAddress();
    logger.info("Server has been started. Serving DocService at http://{}:{}/docs",
        isLocalAddress ? "127.0.0.1" : localAddress.getHostString(), localAddress.getPort());
  }

}
