package com.study;

import com.study.server.NettyServer;


public class App 
{
    public static void main( String[] args )
    {
        new NettyServer().run(8888);


    }

}
