package com.sun.enterprise.ee.cms.core;

/**
 * Created by IntelliJ IDEA.
 * User: sheetal
 * Date: Apr 4, 2008
 * Time: 1:54:34 PM
 * This Exception class has been created to report that the particular
 * member is not in the View
 */
public class MemberNotInViewException extends Exception {

    public MemberNotInViewException(){
        super();
    }

    public MemberNotInViewException(String message){
        super(message);
    }

    public MemberNotInViewException(Throwable e){
        super(e);
    }

    public MemberNotInViewException(String s, Throwable e) {
        super(s, e);
    }
}

