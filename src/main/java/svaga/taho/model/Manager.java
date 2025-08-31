package svaga.taho.model;

import org.springframework.stereotype.Component;

@Component
public class Manager {
    private String managerId;
    private String uid;
    private String approvedManagerUid;

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getApprovedManagerUid() {
        return approvedManagerUid;
    }

    public void setApprovedManagerUid(String approvedManagerUid) {
        this.approvedManagerUid = approvedManagerUid;
    }
}
