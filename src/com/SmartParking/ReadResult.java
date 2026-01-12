package com.SmartParking;

public class ReadResult {
    public final String epcHex;
    public final String dataHex;
    public final int status;     // Linkage 返回值
    public final int rwStatus;   // rw.status

    public ReadResult(String epcHex, String dataHex, int status, int rwStatus) {
        this.epcHex = epcHex;
        this.dataHex = dataHex;
        this.status = status;
        this.rwStatus = rwStatus;
    }

    public boolean ok() { return status == 0 && rwStatus == 0; }
}

