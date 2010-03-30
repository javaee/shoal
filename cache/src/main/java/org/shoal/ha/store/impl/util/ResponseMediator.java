package org.shoal.ha.store.impl.util;

import org.shoal.ha.store.impl.util.CommandResponse;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: mk
 * Date: Jan 9, 2010
 * Time: 2:44:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseMediator {

    private ConcurrentHashMap<Long, CommandResponse> responses =
            new ConcurrentHashMap<Long, CommandResponse>();

    public CommandResponse createCommandResponse(Class type) {
        CommandResponse resp = new CommandResponse(this);
        responses.put(resp.getTokenId(), resp);

        return resp;
    }

    public CommandResponse getCommandResponse(long tokenId) {
        return  responses.get(tokenId);
    }

    public void removeCommandResponse(long id) {
        responses.remove(id);
    }
}
