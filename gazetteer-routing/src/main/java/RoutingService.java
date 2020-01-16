import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import io.gazetteer.routing.Routing.Point;
import io.gazetteer.routing.Routing.RoutingResponse;
import io.gazetteer.routing.RoutingServiceGrpc;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RoutingService extends RoutingServiceGrpc.RoutingServiceImplBase {

  private final GraphHopper graphhopper;

  public RoutingService(GraphHopper graphhopper) {
    this.graphhopper = graphhopper;
  }

  @Override
  public void routing(io.gazetteer.routing.Routing.RoutingRequest request,
      io.grpc.stub.StreamObserver<io.gazetteer.routing.Routing.RoutingResponse> responseObserver) {
    GHRequest req = new GHRequest(request.getLatFrom(), request.getLonFrom(), request.getLatTo(), request.getLonTo())
        .setWeighting("fastest")
        .setVehicle("car")
        .setLocale(Locale.US);
    GHResponse res = graphhopper.route(req);
    if (res.hasErrors()) {
      responseObserver.onError(new Exception("Error!"));
    }
    PathWrapper path = res.getBest();
    RoutingResponse routingResponse = RoutingResponse.newBuilder()
        .addAllPoints(StreamSupport.stream(path.getPoints().spliterator(), false)
            .map(p -> Point.newBuilder().setLon(p.getLon()).setLat(p.getLat()).setEle(p.getEle()).build())
            .collect(Collectors.toList()))
        .setDistance(path.getDistance())
        .setTime(path.getTime())
        .build();
    responseObserver.onNext(routingResponse);
    responseObserver.onCompleted();
  }

}
