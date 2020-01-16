import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.HttpServiceWithRoutes;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.docs.DocServiceBuilder;
import com.linecorp.armeria.server.docs.DocServiceFilter;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.maxmind.geoip2.DatabaseReader;
import io.gazetteer.geoip.GeoIPServiceGrpc;
import io.gazetteer.geoip.Geoip.GeoIPRequest;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;

import java.io.File;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoIPServiceMain {


  private static final Logger logger = LoggerFactory.getLogger(GeoIPServiceMain.class);

  public static void main(String[] args) throws Exception {
    int httpPort = 9090;
    int httpsPort = 9443;
    String geoipFile = "";

    File database = new File(geoipFile);
    DatabaseReader reader = new DatabaseReader.Builder(database).build();

    GeoIPRequest exampleRequest = GeoIPRequest.newBuilder().build();
    HttpServiceWithRoutes grpcService =
        GrpcService.builder()
            .addService(new GeoIPService(reader))
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
            .exampleRequestForMethod(GeoIPServiceGrpc.SERVICE_NAME, "GeoIP", exampleRequest)
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