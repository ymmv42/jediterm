package com.jediterm

import com.jediterm.core.input.MouseEvent
import com.jediterm.terminal.emulator.mouse.MouseButtonCodes
import com.jediterm.terminal.emulator.mouse.MouseEventProcessingSettings
import com.jediterm.util.TestSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SM/RM and DECSET/DECRST accept several modes in one sequence. ncurses relies on it with the
 * xterm-256color terminfo entry, which enables SGR mouse reporting with `CSI ? 1006 ; 1000 h`.
 */
class SetModeParametersTest {

  @Test
  fun `combined DECSET enables both the SGR format and mouse reporting`() {
    val session = TestSession(80, 24)
    session.process("\u001b[?1006;1000h")
    assertEquals("\u001b[<0;3;4M", clickAndGetReport(session))
  }

  @Test
  fun `combined DECSET does not depend on the order of the modes`() {
    val session = TestSession(80, 24)
    session.process("\u001b[?1000;1006h")
    assertEquals("\u001b[<0;3;4M", clickAndGetReport(session))
  }

  @Test
  fun `combined DECRST disables mouse reporting`() {
    val session = TestSession(80, 24)
    session.process("\u001b[?1006;1000h\u001b[?1006;1000l")
    assertEquals("", clickAndGetReport(session))
  }

  @Test
  fun `single DECSET still enables mouse reporting`() {
    val session = TestSession(80, 24)
    session.process("\u001b[?1000h")
    assertTrue(clickAndGetReport(session).isNotEmpty())
  }

  @Test
  fun `combined SM applies every ANSI mode`() {
    val session = TestSession(80, 24)
    assertFalse(session.terminal.isAutoNewLine)
    session.process("\u001b[4;20h")
    assertTrue(session.terminal.isAutoNewLine)
  }

  private fun clickAndGetReport(session: TestSession): String {
    val click = MouseEvent(MouseEvent.Type.PRESSED, MouseButtonCodes.LEFT, 0)
    val settings = MouseEventProcessingSettings(
      isMouseReportingEnabled = true,
      isUsingAlternateBuffer = false,
      isSimulateMouseScrollWithArrowKeysInAlternateScreen = false,
    )
    session.terminal.getOutputAndClear()
    session.terminal.onMouseEvent(2, 3, click, settings)
    return session.terminal.getOutputAndClear()
  }
}
