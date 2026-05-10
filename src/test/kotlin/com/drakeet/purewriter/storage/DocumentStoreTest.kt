/*
 * Copyright (C) 2026 PureWPlus Project Contributors
 *
 * This file is part of Pure Writer Desktop / PureWPlus
 *
 * Pure Writer Desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.drakeet.purewriter.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DocumentStoreTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testSaveAndLoadSingleDocument() {
    val dir = File(tempFolder.root, "docs")
    val store = DocumentStore(documentsDirProvider = { dir })

    val record = store.saveDocument("story_1", "Chapter 1", "Once upon a time", isDirty = false)
    assertEquals("story_1", record.metadata.id)
    assertEquals("Chapter 1", record.metadata.title)
    assertEquals(4, record.metadata.wordCount)
    assertEquals(16, record.metadata.charCount)

    val loaded = store.loadDocument("story_1")
    assertNotNull(loaded)
    assertEquals("Once upon a time", loaded?.content)
    assertEquals("Chapter 1", loaded?.metadata?.title)
  }

  @Test
  fun testListDocumentsAndSorting() {
    val dir = File(tempFolder.root, "docs")
    val store = DocumentStore(documentsDirProvider = { dir })

    store.saveDocument("doc_alpha", "Alpha Chapter", "Content A")
    Thread.sleep(10)
    store.saveDocument("doc_beta", "Beta Chapter", "Content B")

    val docs = store.listDocuments()
    assertEquals(2, docs.size)
    // Most recently modified first
    assertEquals("Beta Chapter", docs[0].title)
    assertEquals("Alpha Chapter", docs[1].title)
  }

  @Test
  fun testDeleteDocument() {
    val dir = File(tempFolder.root, "docs")
    val store = DocumentStore(documentsDirProvider = { dir })

    store.saveDocument("to_delete", "Trash Story", "Temporary text")
    assertTrue(store.deleteDocument("to_delete"))
    assertFalse(store.deleteDocument("to_delete")) // already deleted

    val loaded = store.loadDocument("to_delete")
    assertEquals(null, loaded)
  }

  @Test
  fun testGenerateDocId() {
    val store = DocumentStore()
    val id1 = store.generateDocId("Chapter 1: The Beginning!")
    assertTrue(id1.startsWith("doc_Chapter_1__The_Beginning"))

    val id2 = store.generateDocId("第一章 启程")
    assertTrue(id2.contains("第一章"))
  }
}
