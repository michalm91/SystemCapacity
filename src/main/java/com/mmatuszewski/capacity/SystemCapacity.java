package com.mmatuszewski.capacity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class SystemCapacity {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemCapacity.class);

    private static final int NUMBER_OF_USERS = 5;
    private static final int NUMBER_OF_REQUEST_PER_USER = 1;
    private static final int NUMBER_OF_REQUEST_ALL_USERS = 8;
    private static final int WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND = 1000;
    private static final int FICTITIOUS_WORK_TIME_MILLISECOND = 2000;

    private final Semaphore users;
    private final Semaphore requestAllUsers;
    private final AbstractMap<String, Semaphore> usersActive;

    public SystemCapacity() {
        users = new Semaphore(NUMBER_OF_USERS);
        requestAllUsers = new Semaphore(NUMBER_OF_REQUEST_ALL_USERS);
        usersActive = new ConcurrentHashMap<>();
    }

    //main method
    public boolean work(final String user) {
        if (!canProcess(user)) {
            return false;
        }

        process();
        releaseAllResources(user);
        LOGGER.info(String.format(Message.SUCCESS_PROCESSING, user));
        return true;
    }

    private boolean canProcess(final String user) {
        try {
            if (!isCapacityForRequest()) {
                LOGGER.warn(Message.NO_CAPACITY_REQUEST);
                return false;
            }
            if (!isCapacityForUser(user)) {
                LOGGER.warn(Message.NO_CAPACITY_USER);
                releaseRequests();
                return false;
            }
            if (!isCapacityForUserRequest(user)) {
                LOGGER.warn(Message.NO_CAPACITY_USER_REQUEST);
                releaseRequestsAndUser(user);
                return false;
            }
        } catch (final InterruptedException e) {
            LOGGER.error(Message.INTERNAL_ERROR);
            return false;
        }

        return false;
    }

    private boolean isCapacityForRequest() throws InterruptedException {
        return requestAllUsers.tryAcquire(WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND, TimeUnit.MILLISECONDS);
    }

    private boolean isCapacityForUser(final String user) throws InterruptedException {
        if (!usersActive.containsKey(user)) {
            return users.tryAcquire(WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND, TimeUnit.MILLISECONDS);
        }

        return true;
    }

    private boolean isCapacityForUserRequest(final String user) throws InterruptedException {
        final Semaphore userSemaphore = usersActive.get(user);
        if (userSemaphore != null) {
            final boolean isCapacity = userSemaphore.tryAcquire(WAITING_FOR_ACCESS_TIMEOUT_MILLISECOND, TimeUnit.MILLISECONDS);
            if (!isCapacity) {
                return false;
            }

            usersActive.compute(user, (k, v) -> v = userSemaphore);
        } else {
            final Semaphore newSemaphore = new Semaphore(NUMBER_OF_REQUEST_PER_USER);
            newSemaphore.acquire();
            usersActive.put(user, newSemaphore);
        }
        return true;
    }

    private void process() {
        try {
            LOGGER.info(Message.USER_PROCESSING);
            Thread.sleep(FICTITIOUS_WORK_TIME_MILLISECOND);
        } catch (final InterruptedException e) {
            LOGGER.error(Message.INTERNAL_ERROR);
        }
    }

    private void releaseAllResources(final String user) {
        releaseRequestsAndUser(user);
        if (usersActive.get(user).availablePermits() == NUMBER_OF_REQUEST_PER_USER) {
            usersActive.remove(user);
        }
    }

    private void releaseRequests() {
        requestAllUsers.release();
    }

    private void releaseRequestsAndUser(final String user) {
        releaseRequests();
        users.release();

        final Semaphore userSemaphore = usersActive.get(user);
        if (userSemaphore == null) {
            return;
        }

        userSemaphore.release();
        usersActive.compute(user, (k, v) -> v = userSemaphore);
    }
}
