package serializers.RDMA;

import com.ibm.disni.RdmaActiveEndpoint;
import com.ibm.disni.RdmaActiveEndpointGroup;
import com.ibm.disni.RdmaEndpointFactory;
import com.ibm.disni.verbs.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

public class DataEndpoint extends RdmaActiveEndpoint {
    private IbvMr mr;
    private ArrayBlockingQueue<IbvWC> events = new ArrayBlockingQueue<>(10);

    private SVCPostSend sendSVC;

    private SVCPostSend makeSendSVC() throws IOException{
        IbvSendWR wr = new IbvSendWR();
        wr.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
        wr.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);

        IbvSge sge = new IbvSge();
        sge.setLkey(mr.getLkey());
        sge.setAddr(mr.getAddr());
        sge.setLength(mr.getLength());
        LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
        sgeList.add(sge);
        wr.setSg_list(sgeList);

        LinkedList<IbvSendWR> wrList = new LinkedList<>();
        wrList.add(wr);
        return this.postSend(wrList);
    }
    public void init() throws IOException {
       super.init();
    }

    public DataEndpoint(RdmaActiveEndpointGroup<? extends RdmaActiveEndpoint> group, RdmaCmId idPriv, boolean serverSide) throws IOException {
        super(group, idPriv, serverSide);
    }

    @Override
    public void dispatchCqEvent(IbvWC ibvWC) throws IOException {
        events.add(ibvWC);
    }

    public static class DataEndpointFactory implements RdmaEndpointFactory<DataEndpoint> {
        private RdmaActiveEndpointGroup<DataEndpoint> group;

        public DataEndpointFactory(RdmaActiveEndpointGroup group) {
            this.group = group;
        }
        public DataEndpoint createEndpoint(RdmaCmId idPriv, boolean server) throws IOException {
            return new DataEndpoint(group, idPriv, server);
        }
    }

    public void registerBuffer(ByteBuffer buffer) throws IOException{
        mr = registerMemory(buffer).execute().free().getMr();
        sendSVC = makeSendSVC();
    }

    public void postRead(long address, int rkey, int transferSize) throws IOException {
        SVCPostSend.RdmaMod rdmaMod = sendSVC.getWrMod(0).getRdmaMod();
        rdmaMod.setRemote_addr(address);
        rdmaMod.setRkey(rkey);
        sendSVC.getWrMod(0).getSgeMod(0).setLength(transferSize);
        sendSVC.execute();
    }
    public void waitEvent() throws InterruptedException {
        events.take();
    }

}
