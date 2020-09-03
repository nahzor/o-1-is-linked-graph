# O-1-is-linked-Graph

A graph implementation to do a check to see if two nodes are linked in O(1) time complexity.

Usage:
1. add 1 2
2. remove 1 2
3. is linked 1 2

Assumption:
1. The implementation assumes that there would not be multiple links between 2 same nodes as there is no concept of weights in this situation. If we need to accomodate such a scenario, we could add a count to the neighbor list.
2. The implementation assumes that only integer values would be passed in. So, i have not added checks for non-integer value to discard them as bad input.
  
Note: This is not a thread-safe implementation. 

Complexity:
1. Space Complexity:
  The Graph representation would take O(V+E) space.
  The intNodeMap would take O(V) space.
  The intClusterMap would take O(V) space as each node is mapped to a cluster and each Node could potentially map to its own cluster as worst case.
2. Add link time complexity:
  Add would take O(V) time as it loops over all the clusters once and loops over nodes in one cluster once.
3. Remove link time complexity:
  O(V+E) as we are doing a DFS using adjacency lists.
4. 'Is linked' time complexity:
  O(1) as all the processing is done during add and remove.

