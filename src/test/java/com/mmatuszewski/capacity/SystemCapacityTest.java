package com.mmatuszewski.capacity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SystemCapacityTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemCapacityTest.class);

    private static final List<String> LIST_OF_EXAMPLE_NAMES = Arrays.asList("Jan", "Dionizy", "Roman", "Janusz", "Kot");

    private static final int NUMBER_OF_ACTIVE_THREADS = 3;

    private static final int MINIMUM_DELAY_BETWEEN_THREADS_MILLISECONDS = 10;
    private static final int MAXIMUM_DELAY_BETWEEN_THREADS_MILLISECONDS = 2000;

    @Autowired
    SystemCapacity config;


    @Test
    public void compute_many_request() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(NUMBER_OF_ACTIVE_THREADS);

        for (int iterator = 0; iterator < NUMBER_OF_ACTIVE_THREADS; iterator++)
        {
            executor.schedule(getThreadRandomUser(), randomNumber(MINIMUM_DELAY_BETWEEN_THREADS_MILLISECONDS, MAXIMUM_DELAY_BETWEEN_THREADS_MILLISECONDS), TimeUnit.MILLISECONDS);
        }

        executor.shutdown();
        try
        {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private int randomNumber(int leftLimit, int rightLimit)
    {
        return leftLimit + (int) (new Random().nextFloat() * (rightLimit - leftLimit));
    }

    private Runnable getThreadRandomUser()
    {
        String user = LIST_OF_EXAMPLE_NAMES.get(randomNumber(0,4));
        LOGGER.info(user + " is trying send a request...");

        return () -> LOGGER.info("Have " + user + " had access?: " + config.isAvailabilityToHandlingRequest(user));
    }
}
