package com.example.shizuku.aidl;

import io.github.nguyenduck.autoaidl.annotations.MethodId;
import io.github.nguyenduck.autoaidl.annotations.ShizukuInterface;

@ShizukuInterface
public interface IFile {
    @MethodId(9)
    void testMethodId();

    // Expected cause RuntimeException: Method id is duplicated at PathToThisFile:Line:Column, please use other id!
//    @MethodId(9)
//    void testMethodIdDuplicated();
}
