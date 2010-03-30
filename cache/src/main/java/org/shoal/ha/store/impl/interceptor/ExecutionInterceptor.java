package org.shoal.ha.store.impl.interceptor;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.command.CommandManager;


/**
 * @author Mahesh Kannan
 *
 */
public abstract class ExecutionInterceptor<K, V> {

    private DataStoreContext<K, V> dsc;
    
    private CommandManager<K, V> cm;
    
    private ExecutionInterceptor<K, V> next;
  
    private ExecutionInterceptor<K, V> prev;
    
    public final void initialize(DataStoreContext<K, V> dsc) {
        this.dsc = dsc;
        this.cm = dsc.getCommandManager();
    }

    public final DataStoreContext<K, V> getReplicationService() {
        return dsc;
    }

    public CommandManager getCm() {
        return cm;
    }

    public final void setNext(ExecutionInterceptor<K, V> next) {
        this.next = next;
    }

    public final void setPrev(ExecutionInterceptor<K, V> prev) {
        this.prev = prev;
    }

    public final ExecutionInterceptor<K, V> getNext() {
        return next;    
    }
    
    public final ExecutionInterceptor<K, V> getPrev() {
        return prev;
    }

    public void onTransmit(Command<K, V> cmd) {
        ExecutionInterceptor n = getNext();
        if (n != null) {
            n.onTransmit(cmd);
        }
    }

    public void onReceive(Command<K, V> cmd) {
        ExecutionInterceptor<K, V> p = getPrev();
        if (p != null) {
            p.onTransmit(cmd);
        }    
    }

}
