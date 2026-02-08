package com.wxx.library.service;


import com.wxx.library.dto.BookRenewDTO;

public interface BookRenewService {
    //续借方法
    boolean renewBook(BookRenewDTO renewDTO);
}
