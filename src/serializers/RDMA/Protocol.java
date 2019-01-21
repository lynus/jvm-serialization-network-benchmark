package serializers.RDMA;

import com.ibm.darpc.DaRPCProtocol;

public class Protocol implements DaRPCProtocol<Request, Response> {
    @Override
    public Request createRequest() {
        return new Request();
    }
    @Override
    public Response createResponse() {
        return new Response();
    }
}
