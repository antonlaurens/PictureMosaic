package com.gream.mosaic.datastructures;

public class BinaryTree<T extends BinaryTreeNode<?>> {
	
	protected T root;

	public BinaryTree(T root) {
		this.root = root;
	}
	
}
