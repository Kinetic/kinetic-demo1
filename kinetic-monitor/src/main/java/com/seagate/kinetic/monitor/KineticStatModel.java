package com.seagate.kinetic.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KineticStatModel {
    private Map<String, List<NodeStatItem>> nodesStat;
    private final static int CURRENT = 0;
    private final static int PREVIOUS = 1;

    public KineticStatModel() {
        nodesStat = new HashMap<String, List<NodeStatItem>>();
    }

    public synchronized int getNodeCount() {
        return nodesStat.keySet().size();
    }

    public synchronized void updateNodeStat(String node, double totalPutTimes,
            double totalPutBytes, double totalGetTimes, double totalGetBytes,
            double totalDeleteTimes, double totalDeleteBytes) {
        List<NodeStatItem> nodeStatArray = null;
        if ((nodeStatArray = nodesStat.get(node)) == null) {
            nodeStatArray = new ArrayList<NodeStatItem>();
            nodeStatArray.add(CURRENT, new NodeStatItem(totalPutTimes,
                    totalPutBytes, totalGetTimes, totalGetBytes,
                    totalDeleteTimes, totalDeleteBytes));
            nodeStatArray.add(new NodeStatItem(0, 0, 0, 0, 0, 0, 0));
            nodesStat.put(node, nodeStatArray);
        } else {
            nodeStatArray.add(PREVIOUS, nodeStatArray.get(CURRENT));
            nodeStatArray.add(CURRENT, new NodeStatItem(totalPutTimes,
                    totalPutBytes, totalGetTimes, totalGetBytes,
                    totalDeleteTimes, totalDeleteBytes));
        }
    }

    public synchronized void updateNodeStat(String node, NodeStatItem nodeStat) {
        updateNodeStat(node, nodeStat.getTotalPutTimes(),
                nodeStat.getTotalPutBytes(), nodeStat.getTotalGetTimes(),
                nodeStat.getTotalGetBytes(), nodeStat.getTotalDeleteTimes(),
                nodeStat.getTotalDeleteBytes());
    }

    public synchronized NodeStatItem getAvgSystemStat() {
        int totalNodes = nodesStat.size();
        if (totalNodes == 0) {
            return new NodeStatItem(0, 0, 0, 0, 0, 0);
        }

        double putTimesInSec = 0;
        double putBytesInSec = 0;
        double getTimesInSec = 0;
        double getBytesInSec = 0;
        double deleteTimesInSec = 0;
        double deleteBytesInSec = 0;

        NodeStatItem nodeStat = null;
        for (String node : nodesStat.keySet()) {
            nodeStat = getAvgNodeStat(node);
            putTimesInSec += nodeStat.getTotalPutTimes();
            putBytesInSec += nodeStat.getTotalPutBytes();
            getTimesInSec += nodeStat.getTotalGetTimes();
            getBytesInSec += nodeStat.getTotalGetBytes();
            deleteTimesInSec += nodeStat.getTotalDeleteTimes();
            deleteBytesInSec += nodeStat.getTotalDeleteBytes();
        }

        return new NodeStatItem(putTimesInSec, putBytesInSec, getTimesInSec,
                getBytesInSec, deleteTimesInSec, deleteBytesInSec);
    }

    public synchronized NodeStatItem getCurrentNodeStat(String node) {
        List<NodeStatItem> nodeStatArray = null;
        if ((nodeStatArray = nodesStat.get(node)) == null) {
            return new NodeStatItem(0, 0, 0, 0, 0, 0, 0);
        }

        NodeStatItem current = nodeStatArray.get(CURRENT);
        return current;
    }

    public synchronized NodeStatItem getAvgNodeStat(String node) {
        List<NodeStatItem> nodeStatArray = null;
        if ((nodeStatArray = nodesStat.get(node)) == null) {
            return new NodeStatItem(0, 0, 0, 0, 0, 0, 0);
        }

        NodeStatItem current = nodeStatArray.get(CURRENT);
        NodeStatItem previous = nodeStatArray.get(PREVIOUS);

        if (previous.getRecordTimeInMilliSec() == 0) {
            return new NodeStatItem(0, 0, 0, 0, 0, 0, 0);
        }

        double intervalInSec = (current.getRecordTimeInMilliSec() - previous
                .getRecordTimeInMilliSec()) / 1000;
        double putTimesInSec = (current.getTotalPutTimes() - previous
                .getTotalPutTimes()) / intervalInSec;
        double putBytesInSec = (current.getTotalPutBytes() - previous
                .getTotalPutBytes()) / intervalInSec;
        double getTimesInSec = (current.getTotalGetTimes() - previous
                .getTotalGetTimes()) / intervalInSec;
        double getBytesInSec = (current.getTotalGetBytes() - previous
                .getTotalGetBytes()) / intervalInSec;
        double deleteTimesInSec = (current.getTotalDeleteTimes() - previous
                .getTotalDeleteTimes()) / intervalInSec;
        double deleteBytesInSec = (current.getTotalDeleteBytes() - previous
                .getTotalDeleteBytes()) / intervalInSec;

        return new NodeStatItem(putTimesInSec, putBytesInSec, getTimesInSec,
                getBytesInSec, deleteTimesInSec, deleteBytesInSec);
    }

    public static class NodeStatItem {
        private double totalPutTimes;
        private double totalPutBytes;
        private double totalGetTimes;
        private double totalGetBytes;
        private double totalDeleteTimes;
        private double totalDeleteBytes;

        private double recordTimeInMilliSec;

        public NodeStatItem(double totalPutTimes, double totalPutBytes,
                double totalGetTimes, double totalGetBytes,
                double totalDeleteTimes, double totalDeleteBytes) {
            super();
            this.totalPutTimes = totalPutTimes;
            this.totalPutBytes = totalPutBytes;
            this.totalGetTimes = totalGetTimes;
            this.totalGetBytes = totalGetBytes;
            this.totalDeleteTimes = totalDeleteTimes;
            this.totalDeleteBytes = totalDeleteBytes;

            recordTimeInMilliSec = System.currentTimeMillis();
        }

        public NodeStatItem(double totalPutTimes, double totalPutBytes,
                double totalGetTimes, double totalGetBytes,
                double totalDeleteTimes, double totalDeleteBytes,
                double recordTimeInMilliSec) {
            super();
            this.totalPutTimes = totalPutTimes;
            this.totalPutBytes = totalPutBytes;
            this.totalGetTimes = totalGetTimes;
            this.totalGetBytes = totalGetBytes;
            this.totalDeleteTimes = totalDeleteTimes;
            this.totalDeleteBytes = totalDeleteBytes;
            this.recordTimeInMilliSec = recordTimeInMilliSec;
        }

        public double getTotalPutTimes() {
            return totalPutTimes;
        }

        public void setTotalPutTimes(double totalPutTimes) {
            this.totalPutTimes = totalPutTimes;
        }

        public double getTotalPutBytes() {
            return totalPutBytes;
        }

        public void setTotalPutBytes(double totalPutBytes) {
            this.totalPutBytes = totalPutBytes;
        }

        public double getTotalGetTimes() {
            return totalGetTimes;
        }

        public void setTotalGetTimes(double totalGetTimes) {
            this.totalGetTimes = totalGetTimes;
        }

        public double getTotalGetBytes() {
            return totalGetBytes;
        }

        public void setTotalGetBytes(double totalGetBytes) {
            this.totalGetBytes = totalGetBytes;
        }

        public double getTotalDeleteTimes() {
            return totalDeleteTimes;
        }

        public void setTotalDeleteTimes(double totalDeleteTimes) {
            this.totalDeleteTimes = totalDeleteTimes;
        }

        public double getTotalDeleteBytes() {
            return totalDeleteBytes;
        }

        public void setTotalDeleteBytes(double totalDeleteBytes) {
            this.totalDeleteBytes = totalDeleteBytes;
        }

        public double getRecordTimeInMilliSec() {
            return recordTimeInMilliSec;
        }
    }
}
