package com.mmatuszewski.capacity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class SystemCapacity
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemCapacity.class);

    private static final int NUMBER_OF_USERS = 2;
    private static final int NUMBER_OF_REQUEST_PER_USER = 1;
    private static final int NUMBER_OF_REQUEST_ALL_USERS = 8;

    private static final int WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND = 1000;
    private static final int FICTITIOUS_WORK_TIME_MILLISECOND = 2000;

    private final Semaphore users;
    private final Semaphore requestAllUsers;
    private final AbstractMap<String, Semaphore> usersActive;


    public SystemCapacity()
    {
        users = new Semaphore(NUMBER_OF_USERS);
        requestAllUsers = new Semaphore(NUMBER_OF_REQUEST_ALL_USERS);

        usersActive = new ConcurrentHashMap();
    }

    public boolean isAvailabilityToHandlingRequest(String user)
    {
        try
        {
            if(!isCapacityForRequest())
            {
                return false;
            }

            if(!isCapacityForUser(user))
            {
                releaseRequests();
                return false;
            }

            if(!isCapacityForUserRequest(user))
            {
                releaseRequestsAndUser(user);
                return false;
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return false;
        }

        process();
        releaseAllResources(user);
        LOGGER.info(user + " is finishing his request");
        return true;
    }

    private boolean isCapacityForRequest() throws InterruptedException
    {
        return requestAllUsers.tryAcquire(WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND, TimeUnit.MILLISECONDS);
    }

    private boolean isCapacityForUser(String user) throws InterruptedException
    {
        if(!usersActive.containsKey(user))
            return users.tryAcquire(WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND, TimeUnit.MILLISECONDS);

        return true;
    }

    private boolean isCapacityForUserRequest(String user) throws InterruptedException
    {
        Semaphore userSemaphore = usersActive.get(user);
        if(userSemaphore != null)
        {
            boolean isCapacity = userSemaphore.tryAcquire(WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND, TimeUnit.MILLISECONDS);

            if(!isCapacity) return false;

            usersActive.compute(user, (k, v) -> v = userSemaphore);
        }
        else
        {
            Semaphore newUserSemaphore = new Semaphore(NUMBER_OF_REQUEST_PER_USER);
            newUserSemaphore.acquire();
            usersActive.put(user, newUserSemaphore);
        }

        return true;

    }

    private void process()
    {
        try
        {
            Thread.sleep(FICTITIOUS_WORK_TIME_MILLISECOND);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void releaseAllResources(String user)
    {
        releaseRequestsAndUser(user);

        if(usersActive.get(user).availablePermits() == NUMBER_OF_REQUEST_PER_USER)
            usersActive.remove(user);

    }

    private void releaseRequests()
    {
        requestAllUsers.release();
    }

    private void releaseRequestsAndUser(String user)
    {
        releaseRequests();
        users.release();

        Semaphore userSemaphore = usersActive.get(user);
        if(userSemaphore == null)
            return;

        userSemaphore.release();
        usersActive.compute(user, (k, v) -> v=userSemaphore);
    }

}