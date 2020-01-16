import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;
import io.gazetteer.geoip.GeoIPServiceGrpc;
import io.gazetteer.geoip.Geoip.GeoIPResponse;
import java.net.InetAddress;

public class GeoIPService extends GeoIPServiceGrpc.GeoIPServiceImplBase {

  private final DatabaseReader reader;

  public GeoIPService(DatabaseReader reader) {
    this.reader = reader;
  }

  @Override
  public void geoIP(io.gazetteer.geoip.Geoip.GeoIPRequest request,
      io.grpc.stub.StreamObserver<io.gazetteer.geoip.Geoip.GeoIPResponse> responseObserver) {
    ServiceRequestContext ctx = RequestContext.current();
    InetAddress address = ctx.clientAddress();
    try {
      CityResponse cityResponse = reader.city(address);
      Country country = cityResponse.getCountry();
      Subdivision subdivision = cityResponse.getMostSpecificSubdivision();
      City city = cityResponse.getCity();
      Postal postal = cityResponse.getPostal();
      Location location = cityResponse.getLocation();
      GeoIPResponse response = GeoIPResponse.newBuilder()
              .setCountry(country.getName())
              .setCountryIsoCode(country.getIsoCode())
              .setSubdivision(subdivision.getName())
              .setSubdivisionIsoCode(subdivision.getIsoCode())
              .setCity(city.getName())
              .setPostal(postal.getCode())
              .setLongitude(location.getLongitude())
              .setLatitude(location.getLatitude())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      e.printStackTrace();
      responseObserver.onError(new Exception("Unable to find identify the location."));
      responseObserver.onCompleted();
    }
  }

}
