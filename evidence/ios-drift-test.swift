import XCTest

final class DetoxXCUITestRunner: XCTestCase {
  func testLongPressDrift() throws {
    continueAfterFailure = false
    let app = XCUIApplication(bundleIdentifier: "org.reactjs.native.example.RNMapboxExample")
    app.launchArguments = ["-RCT_jsLocation", "localhost:8082"]
    app.launch()
    let result = app.staticTexts["long-press-result"]
    XCTAssertTrue(result.waitForExistence(timeout: 60), app.debugDescription)
    let map = app.descendants(matching: .any)["long-press-map"].firstMatch
    let start = map.coordinate(withNormalizedOffset: CGVector(dx: 0.75, dy: 0.65))
    let end = start.withOffset(CGVector(dx: 8, dy: 0))
    for mode in ["flyTo", "easeTo", "moveTo"] {
      app.buttons["Reset camera"].tap()
      start.press(forDuration: 1.5, thenDragTo: end, withVelocity: XCUIGestureVelocity(rawValue: 8), thenHoldForDuration: 3)
      let text = result.label
      print("IOS_DRIFT \(mode) \(text)")
      let data = try XCTUnwrap(text.data(using: .utf8))
      let state = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
      let target = try XCTUnwrap(state["target"] as? [Double])
      let actual = try XCTUnwrap(state["center"] as? [Double])
      XCTAssertLessThan(zip(target, actual).map { abs($0 - $1) }.max()!, 0.000001, text)
      // A fresh touch must pan again after the held-touch camera command.
      start.press(forDuration: 0, thenDragTo: start.withOffset(CGVector(dx: -80, dy: 0)), withVelocity: .slow, thenHoldForDuration: 0)
      app.buttons["Read camera"].tap()
      let after = try XCTUnwrap(JSONSerialization.jsonObject(with: result.label.data(using: .utf8)!) as? [String: Any])
      let panned = try XCTUnwrap(after["center"] as? [Double])
      XCTAssertGreaterThan(zip(actual, panned).map { abs($0 - $1) }.max()!, 0.00001)
      app.buttons["Mode: \(mode)"].tap()
    }
  }
}
