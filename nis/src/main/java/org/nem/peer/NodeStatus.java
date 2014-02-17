/**
 * 
 */
package org.nem.peer;

/**
 * Status values
 * - ACTIVE, the node is connected and we exchange data
 * - INACTIVE, the node is not connected
 * - FAILURE, the node is not a NEM node
 * @author Thies1965
 *
 */
public enum NodeStatus {
    ACTIVE, INACTIVE, FAILURE
}
