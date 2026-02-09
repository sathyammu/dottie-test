package com.brimmatech.general.types;


import lombok.Data;

@Data
public class Tuple<X, Y> {
    public final X left;
    public final Y right;
    public Tuple(X left, Y right) {
      this.left = left;
      this.right = right;
    } 
  } 
