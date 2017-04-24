package com.yea.shiro.session.mgt.eis;

import java.io.Serializable;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionIdGenerator;

import com.yea.core.base.id.MongodbIDGennerator;

public class ShiroSessionIdGenerator implements SessionIdGenerator {

    public Serializable generateId(Session session) {
        return MongodbIDGennerator.get().toHexString();
    }
}