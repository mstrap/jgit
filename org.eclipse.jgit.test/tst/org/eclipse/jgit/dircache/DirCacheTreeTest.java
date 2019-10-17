/*
 * Copyright (C) 2008-2009, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.dircache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.FileMode;
import org.junit.Test;

public class DirCacheTreeTest extends RepositoryTestCase {
	@Test
	public void testEmptyCache_NoCacheTree() throws Exception {
		final DirCache dc = db.readDirCache();
		assertNull(dc.getCacheTree(false));
	}

	@Test
	public void testEmptyCache_CreateEmptyCacheTree() throws Exception {
		final DirCache dc = db.readDirCache();
		final DirCacheTree tree = assertCacheTree(dc, true);
		assertCacheTree(dc, true);
		assertSame(tree, assertCacheTree(dc, false));
		assertSame(tree, assertCacheTree(dc, true));
		assertData(tree, "", "", 0, 0, false);
	}

	@Test
	public void testEmptyCache_Clear_NoCacheTree() throws Exception {
		final DirCache dc = db.readDirCache();
		final DirCacheTree tree = assertCacheTree(dc, true);
		dc.clear();
		assertNull(dc.getCacheTree(false));
		assertNotSame(tree, assertCacheTree(dc, true));
	}

	@Test
	public void testSingleSubtree() throws Exception {
		final DirCache dc = db.readDirCache();

		final DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a-", "a/b", "a/c", "a/d", "a0b" });
		final int aFirst = 1;
		final int aLast = 3;

		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}
		b.finish();

		assertNull(dc.getCacheTree(false));
		final DirCacheTree root = assertCacheTree(dc, true);
		assertSame(root, assertCacheTree(dc, true));
		assertData(root, "", "", 1, dc.getEntryCount(), false);

		final DirCacheTree aTree = root.getChild(0);
		assertNotNull(aTree);
		assertSame(aTree, root.getChild(0));
		assertData(aTree, "a", "a/", 0, aLast - aFirst + 1, false);
	}

	@Test
	public void testTwoLevelSubtree() throws Exception {
		final DirCache dc = db.readDirCache();

		final DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a-", "a/b", "a/c/e", "a/c/f", "a/d", "a0b" });
		final int aFirst = 1;
		final int aLast = 4;
		final int acFirst = 2;
		final int acLast = 3;

		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}
		b.finish();

		assertNull(dc.getCacheTree(false));
		final DirCacheTree root = assertCacheTree(dc, true);
		assertSame(root, assertCacheTree(dc, true));
		assertData(root, "", "", 1, dc.getEntryCount(), false);

		final DirCacheTree aTree = root.getChild(0);
		assertNotNull(aTree);
		assertSame(aTree, root.getChild(0));
		assertData(aTree, "a", "a/", 1, aLast - aFirst + 1, false);

		final DirCacheTree acTree = aTree.getChild(0);
		assertNotNull(acTree);
		assertSame(acTree, aTree.getChild(0));
		assertData(acTree, "c", "a/c/", 0, acLast - acFirst + 1, false);
	}

	@Test
	public void testAddSubtreeAtStart() {
		DirCacheEntry[] ents = createDirCacheEntries(new String[] { "b", "c" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 2, false);

		ents = createDirCacheEntries(new String[] { "a/x", "b", "c" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 1, 3, false);
		assertData(root.getChild(0), "a", "a/", 0, 1, false);
	}

	@Test
	public void testAddSubtreeAtMid() {
		DirCacheEntry[] ents = createDirCacheEntries(new String[] { "a", "c" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 2, false);

		ents = createDirCacheEntries(new String[] { "a", "b/x", "c" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 1, 3, false);
		assertData(root.getChild(0), "b", "b/", 0, 1, false);
	}

	@Test
	public void testAddSubtreeAtEnd() {
		DirCacheEntry[] ents = createDirCacheEntries(new String[] { "a", "b" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 2, false);

		ents = createDirCacheEntries(new String[] { "a", "b", "c/x" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 1, 3, false);
		assertData(root.getChild(0), "c", "c/", 0, 1, false);
	}

	@Test
	public void testRemoveObsoleteSubtreeAtStart() {
		DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a/x", "b", "c" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 1, 3, false);
		assertData(root.getChild(0), "a", "a/", 0, 1, false);

		ents = createDirCacheEntries(new String[] { "b", "c" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 2, false);
	}

	@Test
	public void testRemoveObsoleteSubtreeAtMid() {
		DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a", "b/x", "c" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 1, 3, false);
		assertData(root.getChild(0), "b", "b/", 0, 1, false);

		ents = createDirCacheEntries(new String[] { "a", "c" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 2, false);
	}

	@Test
	public void testRemoveObsoleteSubtreeAtEnd() {
		DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a", "b", "c/x" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 1, 3, false);
		assertData(root.getChild(0), "c", "c/", 0, 1, false);

		ents = createDirCacheEntries(new String[] { "a", "b" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 2, false);
	}

	@Test
	public void testRemoveObsoleteSubtrees() {
		DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a", "b/x", "b/y", "c", "d/z", "e" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 2, 6, false);
		assertData(root.getChild(0), "b", "b/", 0, 2, false);
		assertData(root.getChild(1), "d", "d/", 0, 1, false);

		ents = createDirCacheEntries(new String[] { "a", "c", "e" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 0, 3, false);
	}

	@Test
	public void testAddAndRemoveObsoleteSubtreesTwoLevel() {
		DirCacheEntry[] ents = createDirCacheEntries(
				new String[] { "a", "b/m", "b/n/x", "c/o/y", "d/s/z", "e" });
		final DirCacheTree root = new DirCacheTree();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 3, 6, false);

		DirCacheTree b = root.getChild(0);
		assertData(b, "b", "b/", 1, 2, false);
		assertData(b.getChild(0), "n", "b/n/", 0, 1, false);

		final DirCacheTree c = root.getChild(1);
		assertData(c, "c", "c/", 1, 1, false);
		assertData(c.getChild(0), "o", "c/o/", 0, 1, false);

		DirCacheTree d = root.getChild(2);
		assertData(d, "d", "d/", 1, 1, false);
		assertData(d.getChild(0), "s", "d/s/", 0, 1, false);

		ents = createDirCacheEntries(
				new String[] { "a", "b/m", "b/n0/x", "d/r/z", "d/s/z", "e" });
		root.invalidateAll();
		root.validate(ents, ents.length, 0, 0);
		assertData(root, "", "", 2, 6, false);

		b = root.getChild(0);
		assertData(b, "b", "b/", 1, 2, false);
		assertData(b.getChild(0), "n0", "b/n0/", 0, 1, false);

		d = root.getChild(1);
		assertData(d, "d", "d/", 2, 2, false);
		assertData(d.getChild(0), "r", "d/r/", 0, 1, false);
		assertData(d.getChild(1), "s", "d/s/", 0, 1, false);
	}

	/**
	 * We had bugs related to buffer size in the DirCache. This test creates an
	 * index larger than the default BufferedInputStream buffer size. This made
	 * the DirCache unable to read the extensions when index size exceeded the
	 * buffer size (in some cases at least).
	 *
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	@Test
	public void testWriteReadTree() throws CorruptObjectException, IOException {
		final DirCache dc = db.lockDirCache();

		final String A = String.format("a%2000s", "a");
		final String B = String.format("b%2000s", "b");
		final String[] paths = { A + "-", A + "-" + B, A + "/" + B,
				A + "0" + B };
		final DirCacheEntry[] ents = createDirCacheEntries(paths);
		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}

		b.commit();
		DirCache read = db.readDirCache();

		assertEquals(paths.length, read.getEntryCount());
		assertEquals(1, assertCacheTree(dc, true).getChildCount());
	}

	private DirCacheTree assertCacheTree(DirCache cache, boolean build) {
		final DirCacheTree tree = cache.getCacheTree(build);
		assertNotNull(tree);
		return tree;
	}

	private DirCacheEntry[] createDirCacheEntries(String[] paths) {
		final DirCacheEntry[] ents = new DirCacheEntry[paths.length];
		for (int i = 0; i < paths.length; i++) {
			ents[i] = new DirCacheEntry(paths[i]);
			ents[i].setFileMode(FileMode.REGULAR_FILE);
		}
		return ents;
	}

	private void assertData(DirCacheTree tree, String name, String path,
			int childCount, int entrySpan, boolean valid) {
		assertEquals(name, tree.getNameString());
		assertEquals(path, tree.getPathString());
		assertEquals(childCount, tree.getChildCount());
		assertEquals(entrySpan, tree.getEntrySpan());
		assertEquals(valid, tree.isValid());
	}
}
