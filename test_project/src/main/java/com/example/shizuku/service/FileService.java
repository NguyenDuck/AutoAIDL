package com.example.shizuku.service;

import android.os.RemoteException;
import com.example.shizuku.aidl.IFileAIDL;

public class FileService extends IFileAIDL.Stub {
    @Override
    public void testMethodId() throws RemoteException {

    }

//    @Override
//    public void testMethodIdDuplicated() throws RemoteException {
//
//    }

    @Override
    public void destroy() throws RemoteException {
        System.exit(0);
    }
}
