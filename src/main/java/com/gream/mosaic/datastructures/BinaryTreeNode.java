package com.gream.mosaic.datastructures;

public class BinaryTreeNode<T> {

	private BinaryTreeNode<T> parent;
	private BinaryTreeNode<T> left;
	private BinaryTreeNode<T> right;
	
	private T contents;
	
	public BinaryTreeNode(T contents) {
		this.contents = contents;
	}
	
	public BinaryTreeNode(T contents, BinaryTreeNode<T> left, BinaryTreeNode<T> right) {
		this(contents);
		this.left = left;
		this.right = right;		
		this.left.setParent(this);
		this.right.setParent(this);
	}
	
	public BinaryTreeNode(T contents, BinaryTreeNode<T> left, BinaryTreeNode<T> right, BinaryTreeNode<T> parent) {
		this(contents, left, right);
		this.parent = parent;
	}

	public final boolean isRoot() {
		return this.parent == null;
	}
	
	public final boolean isLeaf() {
		return left == null && right == null;
	}

	public T getContents() {
		return contents;
	}

	public void setContents(T contents) {
		this.contents = contents;
	}
	
	public BinaryTreeNode<T> getLeft() {
		return left;
	}

	public void setLeft(BinaryTreeNode<T> left) {
		this.left = left;
		this.left.setParent(this);
	}

	public BinaryTreeNode<T> getRight() {
		return right;
	}

	public void setRight(BinaryTreeNode<T> right) {
		this.right = right;
		this.right.setParent(this);
	}
	
	public void setParent(BinaryTreeNode<T> parent) {
		this.parent = parent;
	}

	public BinaryTreeNode<T> getParent() {
		return this.parent;
	}

	
}
