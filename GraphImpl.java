import java.io.*;
import java.util.*;

/**
 * Driver class.
 * <p>
 * Usage:
 * > add 1 2
 * > remove 1 2
 * > is linked 1 2
 * <p>
 * <p>
 * Assumption:
 * 1. The implementation assumes that there would not be multiple links between 2 same nodes as there is no concept of weights in this situation. If we need to accomodate such a scenario, we could add a count to the neighbor list.
 * 2. The implementation assumes that only integer values would be passed in. So, i have not added checks for non-integer value to discard them as bad input.
 * Note:
 * 1. This is not a thread-safe implementation. 
 * <p>
 * Notes:
 * <p>
 * Q: Space Complexity:
 * A:
 * The Graph representation would take O(V+E) space.
 * The intNodeMap would take O(V) space.
 * The intClusterMap would take O(V) space as each node is mapped to a cluster and each Node could potentially map to its own cluster as worst case.
 * <p>
 * Q: Add link time complexity:
 * A: Add would take O(V) time as it loops over all the clusters once and loops over nodes in one cluster once.
 * <p>
 * Q: Remove link time complexity:
 * A: O(V+E) as we are doing a DFS using adjacency lists.
 * <p>
 * Q: 'Is linked' time complexity:
 * A: O(1) as all the processing is done during add and remove.
 */
class GraphImpl {
    /**
     * Add command prefix.
     */
    private static final String ADD_COMMAND = "add";

    /**
     * Remove command prefix.
     */
    private static final String REMOVE_COMMAND = "remove";

    /**
     * First part of the 'is linked' command prefix.
     */
    private static final String IS_COMMAND = "is";

    /**
     * Second part of the 'is linked' command prefix.
     */
    private static final String LINKED_COMMAND = "linked";

    /**
     * Internal representation of a graph.
     */
    private class Graph {
        /**
         * Internal representation of a graph node.
         * Note - This is not exposed as only the graph needs to know about the node class.
         */
        private class Node {
            /**
             * Value of a graph node.
             * Note - Leaving this as a String so that we dont have to worry about the size of the integer and since no arithmetic operation is performed.
             */
            private String value;

            /**
             * Nodes that are linked to the current node.
             */
            private Set<Node> neighbors;

            /**
             * Constructor. (Hiding the default as it should not be used.)
             *
             * @param v Value of the Node.
             */
            public Node(String v) {
                value = v;
                neighbors = new HashSet<Node>();
            }

            /**
             * Add a neighbor to the current node.
             *
             * @param node Node that is being linked to.
             */
            public void addNeighbor(Node node) {
                neighbors.add(node);
            }

            @Override
            public boolean equals(Object o) {

                // If the object is compared with itself then return true
                if (o == this) {
                    return true;
                }

                // Check if the object is of the same type
                if (!(o instanceof Node)) {
                    return false;
                }

                // Typecast o to Node to compare members.
                Node node = (Node) o;

                // Compare the data members and return accordingly
                return this.value == node.value;
            }

            @Override
            public int hashCode() {
                // Reusing the string's hash code.
                return value.hashCode();
            }
        }

        /**
         * Maps from a node's value to the Node object. This aids in easy retrieval of a Node object.
         * Note - This mapping is needed as nodes could belong to multiple disconnected graphs.
         */
        private Map<String, Node> intNodeMap;

        /**
         * Maps from a node's value to the connected graph it belongs to.
         */
        private Map<String, Set<Node>> intClusterMap;

        /**
         * Constructor.
         */
        public Graph() {
            intNodeMap = new HashMap<String, Node>();
            intClusterMap = new HashMap<String, Set<Node>>();
        }

        /**
         * Utility method to aid in recalibrating the clusters when adding a link. This method combines clusters if necessary.
         *
         * @param a Node being linked.
         * @param b Node being linked.
         */
        private void recalibrateClusterListForAdd(Node a, Node b) {
            Set<Node> clusterA = null;
            Set<Node> clusterB = null;

            // If there are existing clusters for the nodes, retrieve them.
            for (Set<Node> cluster : intClusterMap.values()) {
                if (cluster.contains(a)) {
                    clusterA = cluster;
                }

                if (cluster.contains(b)) {
                    clusterB = cluster;
                }
            }

            if (clusterA == null && clusterB == null) {
                // If there were no clusters for either of the nodes, create a new one.
                clusterA = new HashSet<Node>();

                clusterA.add(a);
                clusterA.add(b);

                // Update the mapping to have the nodes point to their clusters.
                intClusterMap.put(a.value, clusterA);
                intClusterMap.put(b.value, clusterA);

            } else if (clusterA != null && clusterB != null) {
                // If both nodes had clusters, combine the clusters.
                clusterA.addAll(clusterB);

                // Update outdated mappings for nodes in the cluster that will be removed.
                for (Node bnodes : clusterB) {
                    intClusterMap.put(bnodes.value, clusterA);
                }
            } else if (clusterA != null) {
                // Cluster for node b is null. So we add node b to node a's cluster.
                clusterA.add(b);
                intClusterMap.put(b.value, clusterA);
            } else {
                // Cluster for node a is null. So we add node a to node b's cluster.
                clusterB.add(a);
                intClusterMap.put(a.value, clusterB);
            }
        }

        /**
         * Add a link between two nodes.
         *
         * @param a Node to link.
         * @param b Node to link.
         */
        public void addLink(String a, String b) {
            Node nodeA;
            Node nodeB;

            // Retrieve existing node or create a new node if one doesn't exist.
            if (intNodeMap.containsKey(a)) {
                nodeA = intNodeMap.get(a);
            } else {
                nodeA = new Node(a);
            }

            // Retrieve existing node or create a new node if one doesn't exist.
            if (intNodeMap.containsKey(b)) {
                nodeB = intNodeMap.get(b);
            } else {
                nodeB = new Node(b);
            }

            // Update the neighbors list to have the other node in the set.
            nodeA.neighbors.add(nodeB);
            nodeB.neighbors.add(nodeA);

            // Add a mapping from a node value to a Node object.
            intNodeMap.put(a, nodeA);
            intNodeMap.put(b, nodeB);

            // Update cluster mapping to accomodate the new link.
            recalibrateClusterListForAdd(nodeA, nodeB);
        }

        /**
         * Utility to rebuild a cluster for a node using a DFS traversal.
         * Note - Used during a removal of a link when existing mappings become invalid.
         *
         * @param node Node whose connected graph to be traversed.
         */
        private void rebuildCluster(Node node) {
            // Create a new cluster to repplace the old one.
            Set<Node> cluster = new HashSet<Node>();

            // Run a DFS traversal to rebuild the cluster.
            doDFSAdd(node, cluster);

            // Updated mappings for all nodes in the cluster.
            for (Node n : cluster) {
                intClusterMap.put(n.value, cluster);
            }
        }

        /**
         * A recursive DFS to rebuild a cluster.
         *
         * @param node    Node whose neighbors the traversal needs to consider.
         * @param cluster The cluster set to add the traversed nodes to.
         */
        private void doDFSAdd(Node node, Set<Node> cluster) {
            // The cluster check is reused to avoid traversing visited nodes again.
            if (!cluster.contains(node)) {
                cluster.add(node);
                for (Node neighbor : node.neighbors) {
                    doDFSAdd(neighbor, cluster);
                }
            }
        }

        /**
         * Remove a link in the graph.
         *
         * @param a Node to remove link from.
         * @param b Node to remove link from.
         */
        public void removeLink(String a, String b) {
            // A link needs to be removed only if a link exists. This check also confirms that the nodes exist in the graph.
            if (!isLinked(a, b, true)) return;

            Set<Node> cluster = intClusterMap.get(a);
            Node nodeA = intNodeMap.get(a);
            Node nodeB = intNodeMap.get(b);

            // Remove the unlinked nodes from the neighbors lists.
            nodeA.neighbors.remove(nodeB);
            nodeB.neighbors.remove(nodeA);

            // Remove mappings for nodes whose clusters might change.
            for (Node node : cluster) {
                intClusterMap.remove(node.value);
            }

            //Rebuild the cluster for the first node.
            rebuildCluster(nodeA);

            // If there were another link to the second node from the first node, the map would ne updated to have it. So, we need not create a cluster for the second node.
            if (!intClusterMap.containsKey(nodeB.value)) rebuildCluster(nodeB);
        }

        /**
         * Checks if node's are linked.
         *
         * @param a          Node to check linking for.
         * @param b          Node to check linking for.
         * @param isInternal boolean flag to decide if the method should print to console. This will not print to console for internal calls.
         * @return boolean value to show if there is a link.
         */
        public boolean isLinked(String a, String b, boolean isInternal) {
            // The node checks guard for a 'null == null' scenario and the cluster check confirms that the nodes are linked.
            boolean isLinked = (intNodeMap.containsKey(a) && intNodeMap.containsKey(b) && intClusterMap.get(a) == intClusterMap.get(b));
            if (!isInternal) System.out.println((isLinked) ? "true" : "false");

            return isLinked;
        }
    } // Graph

    /**
     * Add a link.
     *
     * @param input tokens for the input command.
     */
    private static void addCommand(String[] input) {
        if (input.length != 3) return;

        graph.addLink(input[1], input[2]);
    }

    /**
     * Remove a link.
     *
     * @param input tokens from the input command.
     */
    private static void removeCommand(String[] input) {
        if (input.length != 3) return;

        graph.removeLink(input[1], input[2]);
    }

    /**
     * Check for a link.
     *
     * @param input tokens from the input command.
     */
    private static void isLinkedCommand(String[] input) {
        if (input.length != 4 || !input[1].toLowerCase().equals(LINKED_COMMAND)) return;

        graph.isLinked(input[2], input[3], false);
    }

    /**
     * Process add, remove, and is linked commands.
     *
     * @param input Command to be processed.
     */
    private static void processCommand(String input) {
        // Looking for only a single space assuming that the input will be sanitized.
        String[] tokens = input.split("\\s");

        // Add and remove command have 2 tokens and the link check has 4 tokens.
        if (tokens.length == 3 || tokens.length == 4) {
            switch (tokens[0].toLowerCase()) {
                case ADD_COMMAND:
                    addCommand(tokens);
                    break;
                case REMOVE_COMMAND:
                    removeCommand(tokens);
                    break;
                case IS_COMMAND:
                    isLinkedCommand(tokens);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Run the commands in interactive mode.
     *
     * @throws java.lang.Exception
     */
    private static void runInteractive() throws java.lang.Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = br.readLine();

        while (input != null && !input.isEmpty()) {
            processCommand(input);
            input = br.readLine();
        }
    }

    /**
     * Instance from a graph held by the driver class.
     */
    private static Graph graph;

    public static void main(String[] args) throws java.lang.Exception {
        //Create an instance of a Graph.
        graph = new GraphImpl().new Graph();

        // Run in interactive mode and take inputs from the console.
        runInteractive();

        // Run pre-defined tests.
        //runTests();
    }

    /**
     * Pre-defined test cases.
     */
    private static void runTests() {
        // Test input 1.
        String[] testSet1 = {"add 1 2",
                "add 2 3",
                "add 3 4",
                "is linked 3 1",
                "remove 3 4",
                "is linked 1 4"};

        // Test input 2.
        String[] testSet2 = {"remove 1 2",
                "note: this shouldn’t crash even though this line doesn’t follow the format",
                "add 1 2",
                "add 2 3",
                "add 1 3",
                "add 3 4",
                "add 5 6",
                "is linked 1 1",
                "is linked 1 4",
                "is linked 5 6",
                "is linked 1 6",
                "remove 1 3",
                "is linked 4 1",
                "remove 5 6",
                "is linked 5 6"};

        // Test input 3.
        String[] testSet3 = {"remove 10000000 20000000",
                "note: add 10000000 20000000 this shouldn't crash even though this line doesn't follow the0 format0",
                "add 10000000 20000000",
                "add 20000000 30000000",
                "add 10000000 30000000",
                "add 30000000 40000000",
                "add 50000000 60000000",
                "is linked 10000000 10000000",
                "is linked 10000000 40000000",
                "is linked 50000000 60000000",
                "is linked 10000000 60000000",
                "remove 10000000 30000000",
                "is linked 40000000 10000000",
                "remove 50000000 60000000",
                "is linked 50000000 60000000",
                "add 50000000 30000000",
                "is linked 50000000 40000000",
                "remove 30000000 40000000",
                "is linked 40000000 50000000",
                "remove 20000000 10000000",
                "is linked 10000000 40000000",
                "is linked 10000000 60000000",
                "is linked 20000000 30000000",
                "is linked 50000000 20000000"};

        // Test input 4.
        String[] testSet4 = {"add 1 2",
                "add 3 4",
                "add 5 6",
                "add 7 8",
                "add 9 10",
                "add 11 12",
                "add 13 14",
                "add 15 16",
                "add 17 18",
                "add 19 20",
                "add 21 22",
                "add 23 24",
                "add 25 26",
                "add 27 28",
                "add 29 30",
                "add 31 32",
                "add 33 34",
                "add 35 36",
                "add 37 38",
                "add 39 40",
                "add 41 42",
                "add 43 44",
                "add 45 46",
                "add 47 48",
                "add 49 50",
                "add 51 52",
                "add 53 54",
                "add 55 56",
                "add 57 58",
                "add 59 60",
                "add 61 62",
                "add 63 64",
                "add 65 66",
                "add 67 68",
                "add 69 70",
                "add 71 72",
                "add 73 74",
                "add 75 76",
                "add 77 78",
                "add 79 80",
                "add 81 82",
                "add 83 84",
                "add 85 86",
                "add 87 88",
                "add 89 90",
                "add 91 92",
                "add 93 94",
                "add 95 96",
                "add 97 98",
                "add 99 100",
                "add 101 102",
                "add 103 104",
                "add 105 106",
                "add 107 108",
                "add 109 110",
                "add 111 112",
                "add 113 114",
                "add 115 116",
                "add 117 118",
                "add 119 120",
                "add 121 122",
                "add 123 124",
                "add 125 126",
                "add 127 128",
                "add 129 130",
                "add 131 132",
                "add 133 134",
                "add 135 136",
                "add 137 138",
                "add 139 140",
                "add 141 142",
                "add 143 144",
                "add 145 146",
                "add 147 148",
                "add 149 150",
                "add 151 152",
                "add 153 154",
                "add 155 156",
                "add 157 158",
                "add 159 160",
                "add 161 162",
                "add 163 164",
                "add 165 166",
                "add 167 168",
                "add 169 170",
                "add 171 172",
                "add 173 174",
                "add 175 176",
                "add 177 178",
                "add 179 180",
                "add 181 182",
                "add 183 184",
                "add 185 186",
                "add 187 188",
                "add 189 190",
                "add 191 192",
                "add 193 194",
                "add 195 196",
                "add 197 198",
                "add 199 200",
                "add 201 202",
                "add 203 204",
                "add 205 206",
                "add 207 208",
                "add 209 210",
                "add 211 212",
                "add 213 214",
                "add 215 216",
                "add 217 218",
                "add 219 220",
                "add 221 222",
                "add 223 224",
                "add 225 226",
                "add 227 228",
                "add 229 230",
                "add 231 232",
                "add 233 234",
                "add 235 236",
                "add 237 238",
                "add 239 240",
                "add 241 242",
                "add 243 244",
                "add 245 246",
                "add 247 248",
                "add 249 250",
                "add 251 252",
                "add 253 254",
                "add 255 256",
                "add 257 258",
                "add 259 260",
                "add 261 262",
                "add 263 264",
                "add 265 266",
                "add 267 268",
                "add 269 270",
                "add 271 272",
                "add 273 274",
                "add 275 276",
                "add 277 278",
                "add 279 280",
                "add 281 282",
                "add 283 284",
                "add 285 286",
                "add 287 288",
                "add 289 290",
                "add 291 292",
                "add 293 294",
                "add 295 296",
                "add 297 298",
                "add 299 300",
                "add 301 302",
                "add 303 304",
                "add 305 306",
                "add 307 308",
                "add 309 310",
                "add 311 312",
                "add 313 314",
                "add 315 316",
                "add 317 318",
                "add 319 320",
                "add 321 322",
                "add 323 324",
                "add 325 326",
                "add 327 328",
                "add 329 330",
                "add 331 332",
                "add 333 334",
                "add 335 336",
                "add 337 338",
                "add 339 340",
                "add 341 342",
                "add 343 344",
                "add 345 346",
                "add 347 348",
                "add 349 350",
                "add 351 352",
                "add 353 354",
                "add 355 356",
                "add 357 358",
                "add 359 360",
                "add 361 362",
                "add 363 364",
                "add 365 366",
                "add 367 368",
                "add 369 370",
                "add 371 372",
                "add 373 374",
                "add 375 376",
                "add 377 378",
                "add 379 380",
                "add 381 382",
                "add 383 384",
                "add 385 386",
                "add 387 388",
                "add 389 390",
                "add 391 392",
                "add 393 394",
                "add 395 396",
                "add 397 398",
                "add 399 400",
                "add 401 402",
                "add 403 404",
                "add 405 406",
                "add 407 408",
                "add 409 410",
                "add 411 412",
                "add 413 414",
                "add 415 416",
                "add 417 418",
                "add 419 420",
                "add 421 422",
                "add 423 424",
                "add 425 426",
                "add 427 428",
                "add 429 430",
                "add 431 432",
                "add 433 434",
                "add 435 436",
                "add 437 438",
                "add 439 440",
                "add 441 442",
                "add 443 444",
                "add 445 446",
                "add 447 448",
                "add 449 450",
                "add 451 452",
                "add 453 454",
                "add 455 456",
                "add 457 458",
                "add 459 460",
                "add 461 462",
                "add 463 464",
                "add 465 466",
                "add 467 468",
                "add 469 470",
                "add 471 472",
                "add 473 474",
                "add 475 476",
                "add 477 478",
                "add 479 480",
                "add 481 482",
                "add 483 484",
                "add 485 486",
                "add 487 488",
                "add 489 490",
                "add 491 492",
                "add 493 494",
                "add 495 496",
                "add 497 498",
                "add 499 500",
                "add 501 502",
                "add 503 504",
                "add 505 506",
                "add 507 508",
                "add 509 510",
                "add 511 512",
                "add 513 514",
                "add 515 516",
                "add 517 518",
                "add 519 520",
                "add 521 522",
                "add 523 524",
                "add 525 526",
                "add 527 528",
                "add 529 530",
                "add 531 532",
                "add 533 534",
                "add 535 536",
                "add 537 538",
                "add 539 540",
                "add 541 542",
                "add 543 544",
                "add 545 546",
                "add 547 548",
                "add 549 550",
                "add 551 552",
                "add 553 554",
                "add 555 556",
                "add 557 558",
                "add 559 560",
                "add 561 562",
                "add 563 564",
                "add 565 566",
                "add 567 568",
                "add 569 570",
                "add 571 572",
                "add 573 574",
                "add 575 576",
                "add 577 578",
                "add 579 580",
                "add 581 582",
                "add 583 584",
                "add 585 586",
                "add 587 588",
                "add 589 590",
                "add 591 592",
                "add 593 594",
                "add 595 596",
                "add 597 598",
                "add 599 600",
                "add 601 602",
                "add 603 604",
                "add 605 606",
                "add 607 608",
                "add 609 610",
                "add 611 612",
                "add 613 614",
                "add 615 616",
                "add 617 618",
                "add 619 620",
                "add 621 622",
                "add 623 624",
                "add 625 626",
                "add 627 628",
                "add 629 630",
                "add 631 632",
                "add 633 634",
                "add 635 636",
                "add 637 638",
                "add 639 640",
                "add 641 642",
                "add 643 644",
                "add 645 646",
                "add 647 648",
                "add 649 650",
                "add 651 652",
                "add 653 654",
                "add 655 656",
                "add 657 658",
                "add 659 660",
                "add 661 662",
                "add 663 664",
                "add 665 666",
                "add 667 668",
                "add 669 670",
                "add 671 672",
                "add 673 674",
                "add 675 676",
                "add 677 678",
                "add 679 680",
                "add 681 682",
                "add 683 684",
                "add 685 686",
                "add 687 688",
                "add 689 690",
                "add 691 692",
                "add 693 694",
                "add 695 696",
                "add 697 698",
                "add 699 700",
                "add 701 702",
                "add 703 704",
                "add 705 706",
                "add 707 708",
                "add 709 710",
                "add 711 712",
                "add 713 714",
                "add 715 716",
                "add 717 718",
                "add 719 720",
                "add 721 722",
                "add 723 724",
                "add 725 726",
                "add 727 728",
                "add 729 730",
                "add 731 732",
                "add 733 734",
                "add 735 736",
                "add 737 738",
                "add 739 740",
                "add 741 742",
                "add 743 744",
                "add 745 746",
                "add 747 748",
                "add 749 750",
                "add 751 752",
                "add 753 754",
                "add 755 756",
                "add 757 758",
                "add 759 760",
                "add 761 762",
                "add 763 764",
                "add 765 766",
                "add 767 768",
                "add 769 770",
                "add 771 772",
                "add 773 774",
                "add 775 776",
                "add 777 778",
                "add 779 780",
                "add 781 782",
                "add 783 784",
                "add 785 786",
                "add 787 788",
                "add 789 790",
                "add 791 792",
                "add 793 794",
                "add 795 796",
                "add 797 798",
                "add 799 800",
                "add 801 802",
                "add 803 804",
                "add 805 806",
                "add 807 808",
                "add 809 810",
                "add 811 812",
                "add 813 814",
                "add 815 816",
                "add 817 818",
                "add 819 820",
                "add 821 822",
                "add 823 824",
                "add 825 826",
                "add 827 828",
                "add 829 830",
                "add 831 832",
                "add 833 834",
                "add 835 836",
                "add 837 838",
                "add 839 840",
                "add 841 842",
                "add 843 844",
                "add 845 846",
                "add 847 848",
                "add 849 850",
                "add 851 852",
                "add 853 854",
                "add 855 856",
                "add 857 858",
                "add 859 860",
                "add 861 862",
                "add 863 864",
                "add 865 866",
                "add 867 868",
                "add 869 870",
                "add 871 872",
                "add 873 874",
                "add 875 876",
                "add 877 878",
                "add 879 880",
                "add 881 882",
                "add 883 884",
                "add 885 886",
                "add 887 888",
                "add 889 890",
                "add 891 892",
                "add 893 894",
                "add 895 896",
                "add 897 898",
                "add 899 900",
                "add 901 902",
                "add 903 904",
                "add 905 906",
                "add 907 908",
                "add 909 910",
                "add 911 912",
                "add 913 914",
                "add 915 916",
                "add 917 918",
                "add 919 920",
                "add 921 922",
                "add 923 924",
                "add 925 926",
                "add 927 928",
                "add 929 930",
                "add 931 932",
                "add 933 934",
                "add 935 936",
                "add 937 938",
                "add 939 940",
                "add 941 942",
                "add 943 944",
                "add 945 946",
                "add 947 948",
                "add 949 950",
                "add 951 952",
                "add 953 954",
                "add 955 956",
                "add 957 958",
                "add 959 960",
                "add 961 962",
                "add 963 964",
                "add 965 966",
                "add 967 968",
                "add 969 970",
                "add 971 972",
                "add 973 974",
                "add 975 976",
                "add 977 978",
                "add 979 980",
                "add 981 982",
                "add 983 984",
                "add 985 986",
                "add 987 988",
                "add 989 990",
                "add 991 992",
                "add 993 994",
                "add 995 996",
                "add 997 998",
                "add 999 1000",
                "add 1001 1002",
                "add 1003 1004",
                "add 1005 1006",
                "add 1007 1008",
                "add 1009 1010",
                "add 1011 1012",
                "add 1013 1014",
                "add 1015 1016",
                "add 1017 1018",
                "add 1019 1020",
                "add 1021 1022",
                "add 1023 1024",
                "add 1025 1026",
                "add 1027 1028",
                "add 1029 1030",
                "add 1031 1032",
                "add 1033 1034",
                "add 1035 1036",
                "add 1037 1038",
                "add 1039 1040",
                "add 1041 1042",
                "add 1043 1044",
                "add 1045 1046",
                "add 1047 1048",
                "add 1049 1050",
                "add 1051 1052",
                "add 1053 1054",
                "add 1055 1056",
                "add 1057 1058",
                "add 1059 1060",
                "add 1061 1062",
                "add 1063 1064",
                "add 1065 1066",
                "add 1067 1068",
                "add 1069 1070",
                "add 1071 1072",
                "add 1073 1074",
                "add 1075 1076",
                "add 1077 1078",
                "add 1079 1080",
                "add 1081 1082",
                "add 1083 1084",
                "add 1085 1086",
                "add 1087 1088",
                "add 1089 1090",
                "add 1091 1092",
                "add 1093 1094",
                "add 1095 1096",
                "add 1097 1098",
                "add 1099 1100",
                "add 1101 1102",
                "add 1103 1104",
                "add 1105 1106",
                "add 1107 1108",
                "add 1109 1110",
                "add 1111 1112",
                "add 1113 1114",
                "add 1115 1116",
                "add 1117 1118",
                "add 1119 1120",
                "add 1121 1122",
                "add 1123 1124",
                "add 1125 1126",
                "add 1127 1128",
                "add 1129 1130",
                "add 1131 1132",
                "add 1133 1134",
                "add 1135 1136",
                "add 1137 1138",
                "add 1139 1140",
                "add 1141 1142",
                "add 1143 1144",
                "add 1145 1146",
                "add 1147 1148",
                "add 1149 1150",
                "add 1151 1152",
                "add 1153 1154",
                "add 1155 1156",
                "add 1157 1158",
                "add 1159 1160",
                "add 1161 1162",
                "add 1163 1164",
                "add 1165 1166",
                "add 1167 1168",
                "add 1169 1170",
                "add 1171 1172",
                "add 1173 1174",
                "add 1175 1176",
                "add 1177 1178",
                "add 1179 1180",
                "add 1181 1182",
                "add 1183 1184",
                "add 1185 1186",
                "add 1187 1188",
                "add 1189 1190",
                "add 1191 1192",
                "add 1193 1194",
                "add 1195 1196",
                "add 1197 1198",
                "add 1199 1200",
                "add 1201 1202",
                "add 1203 1204",
                "add 1205 1206",
                "add 1207 1208",
                "add 1209 1210",
                "add 1211 1212",
                "add 1213 1214",
                "add 1215 1216",
                "add 1217 1218",
                "add 1219 1220",
                "add 1221 1222",
                "add 1223 1224",
                "add 1225 1226",
                "add 1227 1228",
                "add 1229 1230",
                "add 1231 1232",
                "add 1233 1234",
                "add 1235 1236",
                "add 1237 1238",
                "add 1239 1240",
                "add 1241 1242",
                "add 1243 1244",
                "add 1245 1246",
                "add 1247 1248",
                "add 1249 1250",
                "add 1251 1252",
                "add 1253 1254",
                "add 1255 1256",
                "add 1257 1258",
                "add 1259 1260",
                "add 1261 1262",
                "add 1263 1264",
                "add 1265 1266",
                "add 1267 1268",
                "add 1269 1270",
                "add 1271 1272",
                "add 1273 1274",
                "add 1275 1276",
                "add 1277 1278",
                "add 1279 1280",
                "add 1281 1282",
                "add 1283 1284",
                "add 1285 1286",
                "add 1287 1288",
                "add 1289 1290",
                "add 1291 1292",
                "add 1293 1294",
                "add 1295 1296",
                "add 1297 1298",
                "add 1299 1300",
                "add 1301 1302",
                "add 1303 1304",
                "add 1305 1306",
                "add 1307 1308",
                "add 1309 1310",
                "add 1311 1312",
                "add 1313 1314",
                "add 1315 1316",
                "add 1317 1318",
                "add 1319 1320",
                "add 1321 1322",
                "add 1323 1324",
                "add 1325 1326",
                "add 1327 1328",
                "add 1329 1330",
                "add 1331 1332",
                "add 1333 1334",
                "add 1335 1336",
                "add 1337 1338",
                "add 1339 1340",
                "add 1341 1342",
                "add 1343 1344",
                "add 1345 1346",
                "add 1347 1348",
                "add 1349 1350",
                "add 1351 1352",
                "add 1353 1354",
                "add 1355 1356",
                "add 1357 1358",
                "add 1359 1360",
                "add 1361 1362",
                "add 1363 1364",
                "add 1365 1366",
                "add 1367 1368",
                "add 1369 1370",
                "add 1371 1372",
                "add 1373 1374",
                "add 1375 1376",
                "add 1377 1378",
                "add 1379 1380",
                "add 1381 1382",
                "add 1383 1384",
                "add 1385 1386",
                "add 1387 1388",
                "add 1389 1390",
                "add 1391 1392",
                "add 1393 1394",
                "add 1395 1396",
                "add 1397 1398",
                "add 1399 1400",
                "add 1401 1402",
                "add 1403 1404",
                "add 1405 1406",
                "add 1407 1408",
                "add 1409 1410",
                "add 1411 1412",
                "add 1413 1414",
                "add 1415 1416",
                "add 1417 1418",
                "add 1419 1420",
                "add 1421 1422",
                "add 1423 1424",
                "add 1425 1426",
                "add 1427 1428",
                "add 1429 1430",
                "add 1431 1432",
                "add 1433 1434",
                "add 1435 1436",
                "add 1437 1438",
                "add 1439 1440",
                "add 1441 1442",
                "add 1443 1444",
                "add 1445 1446",
                "add 1447 1448",
                "add 1449 1450",
                "add 1451 1452",
                "add 1453 1454",
                "add 1455 1456",
                "add 1457 1458",
                "add 1459 1460",
                "add 1461 1462",
                "add 1463 1464",
                "add 1465 1466",
                "add 1467 1468",
                "add 1469 1470",
                "add 1471 1472",
                "add 1473 1474",
                "add 1475 1476",
                "add 1477 1478",
                "add 1479 1480",
                "add 1481 1482",
                "add 1483 1484",
                "add 1485 1486",
                "add 1487 1488",
                "add 1489 1490",
                "add 1491 1492",
                "add 1493 1494",
                "add 1495 1496",
                "add 1497 1498",
                "add 1499 1500",
                "add 1501 1502",
                "add 1503 1504",
                "add 1505 1506",
                "add 1507 1508",
                "add 1509 1510",
                "add 1511 1512",
                "add 1513 1514",
                "add 1515 1516",
                "add 1517 1518",
                "add 1519 1520",
                "add 1521 1522",
                "add 1523 1524",
                "add 1525 1526",
                "add 1527 1528",
                "add 1529 1530",
                "add 1531 1532",
                "add 1533 1534",
                "add 1535 1536",
                "add 1537 1538",
                "add 1539 1540",
                "add 1541 1542",
                "add 1543 1544",
                "add 1545 1546",
                "add 1547 1548",
                "add 1549 1550",
                "add 1551 1552",
                "add 1553 1554",
                "add 1555 1556",
                "add 1557 1558",
                "add 1559 1560",
                "add 1561 1562",
                "add 1563 1564",
                "add 1565 1566",
                "add 1567 1568",
                "add 1569 1570",
                "add 1571 1572",
                "add 1573 1574",
                "add 1575 1576",
                "add 1577 1578",
                "add 1579 1580",
                "add 1581 1582",
                "add 1583 1584",
                "add 1585 1586",
                "add ..."};

        // Switch testSet1 with the test to run.
        String[] input = testSet1;
        for (int i = 0; i < input.length; i++) {
            String command = input[i];
            processCommand(command);
        }
    }
}
