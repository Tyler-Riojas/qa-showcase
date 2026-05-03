package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WaitUtils;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

public class EmployeeListPage {

    private static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

    private final WebDriver driver;

    // The Add button on the Employee List page is type="button" (not type="submit").
    // The Search button is also oxd-button--secondary but has type="submit", so
    // filtering by type="button" uniquely targets the Add button.
    private final By addEmployeeButton  = By.cssSelector("button.oxd-button--secondary[type='button']");
    // Employee Name search uses an autocomplete widget — the input has a specific placeholder
    private final By employeeNameSearch = By.cssSelector(".oxd-autocomplete-text-input input");
    private final By searchButton       = By.cssSelector("button[type=submit]");
    private final By tableRows          = By.cssSelector(".oxd-table-row");
    // The trash icon is an <i> inside a button; target the button that contains it
    private final By deleteButtons      = By.cssSelector("button.oxd-icon-button .bi-trash");
    // OrangeHRM's confirm-delete dialog uses a button with class oxd-button--label-danger
    private final By confirmDeleteBtn   = By.cssSelector(".orangehrm-modal-footer .oxd-button--label-danger");

    public EmployeeListPage(WebDriver driver) {
        this.driver = driver;
    }

    public void clickAddEmployee() {
        log.info("Clicking Add Employee");
        WaitUtils.waitForElementClickable(driver, addEmployeeButton, 15).click();
        WaitUtils.waitForUrlContains(driver, "/addEmployee", 20);
    }

    public void searchEmployee(String firstName, String lastName) {
        log.info("Searching for: {} {}", firstName, lastName);
        try {
            // OrangeHRM uses an autocomplete widget — type to trigger dropdown, then
            // select the matching option before submitting, otherwise the filter is ignored.
            WebElement nameField = WaitUtils.waitForElementVisible(driver, employeeNameSearch, 8);
            nameField.clear();
            nameField.sendKeys(firstName);

            By autocompleteOption = By.cssSelector(".oxd-autocomplete-option");
            try {
                WebElement option = WaitUtils.waitForElementVisible(driver, autocompleteOption, 5);
                option.click();
            } catch (Exception e) {
                log.debug("No autocomplete option appeared — proceeding with search");
            }
        } catch (Exception e) {
            log.warn("Search field not accessible: {}", e.getMessage());
        }
        WaitUtils.waitForElementClickable(driver, searchButton).click();
        WaitUtils.waitForPageLoad(driver);
        // Brief wait for Vue to re-render results after the search
        try { Thread.sleep(1500); } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Returns number of data rows (excludes header row). */
    public int getEmployeeCount() {
        try {
            List<WebElement> rows = WaitUtils.waitForAllElementsVisible(driver, tableRows, 8);
            return Math.max(0, rows.size() - 1);
        } catch (Exception e) {
            log.debug("Could not count rows: {}", e.getMessage());
            return 0;
        }
    }

    /** Clicks the trash icon on the first employee row, then confirms in the modal. */
    public void deleteFirstEmployee() {
        log.info("Deleting first employee in table");
        try {
            List<WebElement> icons = WaitUtils.waitForAllElementsVisible(driver, deleteButtons, 10);
            if (!icons.isEmpty()) {
                // Click the icon's parent button to ensure the click registers
                org.openqa.selenium.WebElement btn = icons.get(0).findElement(By.xpath("./ancestor::button[1]"));
                btn.click();
                WaitUtils.waitForElementClickable(driver, confirmDeleteBtn).click();
                WaitUtils.waitForPageLoad(driver);
            }
        } catch (Exception e) {
            log.warn("Could not delete employee: {}", e.getMessage());
        }
    }

    /** Case-insensitive search across all row text. */
    public boolean isEmployeeInList(String name) {
        try {
            List<WebElement> rows = WaitUtils.waitForAllElementsVisible(driver, tableRows, 8);
            for (WebElement row : rows) {
                if (row.getText().toLowerCase().contains(name.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Error scanning employee list: {}", e.getMessage());
        }
        return false;
    }
}
