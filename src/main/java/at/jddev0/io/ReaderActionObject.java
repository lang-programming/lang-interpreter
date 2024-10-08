package at.jddev0.io;

import java.io.Serializable;

/**
 * IO-Module
 *
 * @author JDDev0
 * @version v0.1
 */
@FunctionalInterface
public interface ReaderActionObject extends Serializable {
    void action(String[] args);
}