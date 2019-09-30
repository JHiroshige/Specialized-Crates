package me.ztowne13.customcrates.interfaces.sql;

import java.util.concurrent.CopyOnWriteArrayList;

public class SQLQueryThread extends Thread
{
    public static volatile CopyOnWriteArrayList<String> sql_query = new CopyOnWriteArrayList();
    public static volatile CopyOnWriteArrayList<Runnable> task_query = new CopyOnWriteArrayList();

    SQL sql;

    public SQLQueryThread(SQL sql)
    {
        this.sql = sql;
        start();
        setName("SpecializedCrates-SQL");

        sql.sc.getDu().log("SQLQueryThread() - Opening connection...", getClass());
        long curTime = System.currentTimeMillis();

        sql.getSqlc().open();

        sql.sc.getDu().log("SQLQueryThread() - Completed opening connection in " + (System.currentTimeMillis() - curTime) + "ms.", getClass());
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                Thread.sleep(250);
            }
            catch (InterruptedException exc)
            {

            }

            for (String query : sql_query)
            {
                sql.sc.getDu().log("run() - query: " + query, getClass());
                try
                {
                    sql.getSqlc().get().prepareStatement(query).executeUpdate();
                }
                catch (Exception exc)
                {
                    //new SQLLog("Failed query: " + query);
                    exc.printStackTrace();
                }

                sql_query.remove(query);
            }

            for(Runnable query : task_query)
            {
                sql.sc.getDu().log("run() - query: " + query.toString(), getClass());

                query.run();
                task_query.remove(query);
            }
        }
    }

    public static void addQuery(String query)
    {
        sql_query.add(query);
    }

    public static void addQuery(Runnable runnable)
    {
        task_query.add(runnable);
    }
}