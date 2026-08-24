//
//  iosAppUITests.swift
//  iosAppUITests
//
//  Created by Vinesh Raju Radhakrishnan on 27/02/2026.
//

import XCTest

final class iosAppUITests: XCTestCase {
    private let rootComposeViewIdentifier = "RootComposeView"

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testCaptureGameScreenScreenshot() throws {
        try captureScreenshot(named: "01_GameScreen", scenario: nil)
    }

    @MainActor
    func testCaptureGameplayScreenshot() throws {
        try captureScreenshot(named: "02_Gameplay", scenario: "gameplay")
    }

    @MainActor
    func testCaptureGameOverScreenshot() throws {
        try captureScreenshot(named: "03_GameOver", scenario: "game-over")
    }

    @MainActor
    func testCaptureSettingsScreenshot() throws {
        try captureScreenshot(named: "04_Settings", scenario: "settings")
    }

    @MainActor
    private func captureScreenshot(named name: String, scenario: String?) throws {
        let app = XCUIApplication()
        setupSnapshot(app)
        if let scenario {
            app.launchArguments += ["-screenshot-scenario", scenario]
        }
        app.launch()

        XCTAssertTrue(app.otherElements[rootComposeViewIdentifier].waitForExistence(timeout: 5))
        snapshot(name, timeWaitingForIdle: 0)
    }
}
