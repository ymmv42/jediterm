package com.jediterm;

import com.jediterm.util.TestSession;
import junit.framework.TestCase;

import java.io.IOException;

public class RepeatCharacterTest extends TestCase {

  public void testRepeatPrecedingCharacter() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("a\u001b[4b");
    assertEquals("a".repeat(5) + " ".repeat(5) + "\n" +
                 " ".repeat(10) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testOmittedCountRepeatsOnce() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("a\u001b[b");
    assertEquals("aa" + " ".repeat(8) + "\n" +
                 " ".repeat(10) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testZeroCountRepeatsOnce() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("a\u001b[0b");
    assertEquals("aa" + " ".repeat(8) + "\n" +
                 " ".repeat(10) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testRepeatWithoutPrecedingCharacterIsIgnored() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("\u001b[5b");
    assertEquals(" ".repeat(10) + "\n" +
                 " ".repeat(10) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testRepeatUsesLastCharacterOfARun() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("ab\u001b[3b");
    assertEquals("ab" + "b".repeat(3) + " ".repeat(5) + "\n" +
                 " ".repeat(10) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  /** What ncurses emits for a 37 character run once the terminfo entry declares `rep`. */
  public void testSeparatorLineEmittedByNcurses() throws IOException {
    TestSession session = new TestSession(39, 2);
    session.process("+-\u001b[36b+");
    assertEquals("+" + "-".repeat(37) + "+\n" +
                 " ".repeat(39) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testRepeatWrapsToTheNextLine() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("a\u001b[11b");
    assertEquals("a".repeat(10) + "\n" +
                 "aa" + " ".repeat(8) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testRepeatCountIsClampedToTheScreenSize() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("a\u001b[2000000000b");
    long written = session.getTerminalTextBuffer().getScreenLines().chars().filter(c -> c == 'a').count();
    assertTrue("expected the repeat count to be clamped, got " + written, written > 0 && written <= 20);
  }

  /** The argument parser overflows above Integer.MAX_VALUE: 3000000000 is read as a negative count. */
  public void testOverflowingRepeatCountIsHandledLikeZero() throws IOException {
    TestSession session = new TestSession(10, 2);
    session.process("a\u001b[3000000000bZ");
    assertEquals("aaZ" + " ".repeat(7) + "\n" +
                 " ".repeat(10) + "\n", session.getTerminalTextBuffer().getScreenLines());
  }

  public void testRepeatLineDrawingCharacter() throws IOException {
    assertRendersLike("\u001b(0qqqqq\u001b(B", "\u001b(0q\u001b[4b\u001b(B");
  }

  public void testRepeatSupplementaryCharacter() throws IOException {
    assertRendersLike("\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00", "\uD83D\uDE00\u001b[2b");
  }

  public void testRepeatDoubleWidthCharacterWrapsLikePlainText() throws IOException {
    assertRendersLike("\u4E2D".repeat(6), "\u4E2D\u001b[5b");
  }

  public void testRepeatCombiningSequence() throws IOException {
    assertRendersLike("e\u0301e\u0301e\u0301", "e\u0301\u001b[2b");
  }

  public void testRepeatAfterFullResetIsIgnored() throws IOException {
    assertRendersLike("", "a\u001bc\u001b[3b");
  }

  /** REP must give exactly what the application would get by sending the repeated text itself. */
  private static void assertRendersLike(String expectedInput, String input) throws IOException {
    TestSession expected = new TestSession(10, 2);
    expected.process(expectedInput);
    TestSession actual = new TestSession(10, 2);
    actual.process(input);
    assertEquals(expected.getTerminalTextBuffer().getScreenLines(), actual.getTerminalTextBuffer().getScreenLines());
    assertEquals(expected.getTerminal().getCursorX(), actual.getTerminal().getCursorX());
    assertEquals(expected.getTerminal().getCursorY(), actual.getTerminal().getCursorY());
  }
}
