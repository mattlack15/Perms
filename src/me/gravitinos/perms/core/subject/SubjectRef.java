package me.gravitinos.perms.core.subject;

import java.lang.ref.WeakReference;

public class SubjectRef {
    private WeakReference<Subject<? extends SubjectData>> reference;
    public SubjectRef(Subject<? extends SubjectData> subject){
        this.reference = new WeakReference<>(subject);
    }
    public Subject<? extends SubjectData> get(){
        return this.reference.get();
    }
    public void setReference(Subject<? extends SubjectData> subj){
        this.reference = new WeakReference<>(subj);
    }
}
