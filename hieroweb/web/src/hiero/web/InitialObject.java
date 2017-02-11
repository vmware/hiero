package hiero.web;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hiero.sketch.dataset.LocalDataSet;
import org.hiero.sketch.dataset.ParallelDataSet;
import org.hiero.sketch.dataset.api.IDataSet;
import org.hiero.sketch.table.Table;
import org.hiero.sketch.table.api.ITable;

import javax.websocket.Session;
import java.util.ArrayList;

public class InitialObject extends RpcTarget {
    @HieroRpc
    void loadTable(@NonNull RpcRequest request, @NonNull Session session) {
        // TODO: look at request.  Now we just supply always the same table
        Table t = Table.testTable();
        final int parts = 5;
        ArrayList<IDataSet<ITable>> fragments = new ArrayList<IDataSet<ITable>>();
        for (int i = 0; i < parts; i++) {
            LocalDataSet<ITable> data = new LocalDataSet<ITable>(t);
            fragments.add(data);
        }
        IDataSet<ITable> big = new ParallelDataSet<ITable>(fragments);
        TableTarget table = new TableTarget(big);
        RpcObjectManager.instance.addObject(table);

        RpcReply reply = request.createReply(table.idToJson());
        reply.send(session);
        request.closeSession(session);
    }

    @Override
    public String toString() {
        return "Initial object, " + super.toString();
    }
}
