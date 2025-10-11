(function() {
'use strict';

const API_BASE_URL = '/api/entries';
const MAX_ENTRY_ID = 999999;
const MESSAGE_AUTO_HIDE_DELAY = 5000;
const SCROLL_ANIMATION_DELAY = 300;
const HIGHLIGHT_DURATION = 1000;

let loadedEntryIds = []; // Sorted list of loaded entry IDs
let nameMap = new Map();

// Show loading
function showLoading() {
    $('#results').html('<p class="loading">Loading...</p>');
    $('#resultsCount').text('');
}

// Display results
function displayResults(entries) {
    const resultsDiv = $('#results');
    resultsDiv.empty();

    $('#resultsCount').text(`(${entries.length})`);

    // Clear selected entry field
    $('#selectedEntryId').val('');

    if (entries.length === 0) {
        resultsDiv.html('<p class="loading">No entries found</p>');
        loadedEntryIds = [];
        nameMap = new Map();
        return;
    }

    // Update loadedEntryIds with sorted unique IDs
    loadedEntryIds = entries
        .map(entry => entry.entryId)
        .filter(id => id !== null && id !== undefined)
        .sort((a, b) => a - b);

    entries.forEach(function(entry) {
        nameMap.set(entry.entryId, entry.person.firstName + " " + entry.person.lastName);
        const card = createEntryCard(entry);
        resultsDiv.append(card);
    });
}

// Helper function to extract error message from response
function getErrorMessage(xhr, defaultError) {
    if (xhr.responseJSON && xhr.responseJSON.message) {
        return xhr.responseJSON.error + ': ' + xhr.responseJSON.message;
    }
    return defaultError;
}

// Search by entry ID
function searchByEntryId(entryId) {
    showLoading();
    $.ajax({
        url: `${API_BASE_URL}/${entryId}`,
        method: 'GET',
        success: function(data) {
            displayResults(data);
            if (data.length > 0) {
                showMessage(`Found ${data.length} entry(ies)`, 'success');
            } else {
                showMessage('No entries found', 'info');
            }
        },
        error: function(xhr, status, error) {
            if (xhr.status === 404) {
                showMessage('No entry found with ID: ' + entryId, 'info');
                $('#results').empty();
            } else {
                const errorMsg = getErrorMessage(xhr, 'Error searching entries: ' + error);
                showMessage(errorMsg, 'error');
                $('#results').empty();
            }
        }
    });
}

// Search by last name
function searchByLastName(lastName) {
    showLoading();
    $.ajax({
        url: `${API_BASE_URL}/search/lastName/${encodeURIComponent(lastName)}`,
        method: 'GET',
        success: function(data) {
            displayResults(data);
            if (data.length > 0) {
                showMessage(`Found ${data.length} entry(ies)`, 'success');
            } else {
                showMessage('No entries found', 'info');
            }
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error searching entries: ' + error);
            showMessage(errorMsg, 'error');
            $('#results').empty();
        }
    });
}

// Search by first and last name
function searchByFullName(firstName, lastName) {
    showLoading();
    $.ajax({
        url: `${API_BASE_URL}/search/name/${encodeURIComponent(firstName)}/${encodeURIComponent(lastName)}`,
        method: 'GET',
        success: function(data) {
            displayResults(data);
            if (data.length > 0) {
                showMessage(`Found ${data.length} entry(ies)`, 'success');
            } else {
                showMessage('No entries found', 'info');
            }
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error searching entries: ' + error);
            showMessage(errorMsg, 'error');
            $('#results').empty();
        }
    });
}

// Helper function to escape HTML to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Create entry card HTML
function createEntryCard(entry) {
    const person = entry.person || {};
    const address = entry.address || {};

    const entryId = escapeHtml(String(entry.entryId || 'N/A'));
    const fullName = escapeHtml(`${person.firstName || ''} ${person.lastName || ''}`);

    let html = `<div class="entry-card" data-entry-id="${escapeHtml(String(entry.entryId || ''))}">`;
    html += '<div class="entry-header">';
    html += `<div class="entry-id">ID: ${entryId}</div>`;
    html += `<div class="entry-name">${fullName}</div>`;
    html += '</div>';

    // Person details
    html += '<div class="entry-section">';
    html += '<div class="entry-section-title">Person Details</div>';
    if (person.age) {
        html += `<div class="entry-detail"><span class="entry-detail-label">Age:</span><span class="entry-detail-value">&nbsp;${escapeHtml(String(person.age))}</span></div>`;
    }
    if (person.gender) {
        html += `<div class="entry-detail"><span class="entry-detail-label">Gender:</span><span class="entry-detail-value">&nbsp;${escapeHtml(person.gender)}</span></div>`;
    }
    if (person.maritalStatus) {
        html += `<div class="entry-detail"><span class="entry-detail-label">Marital Status:</span><span class="entry-detail-value">&nbsp;${escapeHtml(person.maritalStatus)}</span></div>`;
    }
    html += '</div>';

    // Address details
    if (address.street || address.city || address.state || address.zip || address.email || address.phone) {
        html += '<div class="entry-section">';
        html += '<div class="entry-section-title">Contact Information</div>';
        if (address.street) {
            html += `<div class="entry-detail"><span class="entry-detail-label">Street:</span><span class="entry-detail-value">${escapeHtml(address.street)}</span></div>`;
        }
        if (address.city || address.state || address.zip) {
            let cityStateZip = [address.city, address.state, address.zip].filter(Boolean).join(', ');
            html += `<div class="entry-detail"><span class="entry-detail-label">Location:</span><span class="entry-detail-value">${escapeHtml(cityStateZip)}</span></div>`;
        }
        if (address.email) {
            html += `<div class="entry-detail"><span class="entry-detail-label">Email:</span><span class="entry-detail-value">${escapeHtml(address.email)}</span></div>`;
        }
        if (address.phone) {
            html += `<div class="entry-detail"><span class="entry-detail-label">Phone:</span><span class="entry-detail-value">${escapeHtml(address.phone)}</span></div>`;
        }
        html += '</div>';
    }

    // Notes
    if (entry.notes) {
        html += '<div class="entry-section">';
        html += '<div class="entry-section-title">Notes</div>';
        html += `<div class="entry-notes">${escapeHtml(entry.notes)}</div>`;
        html += '</div>';
    }

    html += '</div>';
    return html;
}

// Show add form
function showAddForm() {
    // Calculate next unique entryId
    let nextId = 1;
    if (loadedEntryIds.length > 0) {
        const maxId = loadedEntryIds[loadedEntryIds.length - 1];
        nextId = maxId + 1;
    }

    // Prepopulate the entryId field
    $('#entryId').val(nextId);

    $('#addEntrySection').removeClass('hidden');
    $('#resultsSection').addClass('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Hide add form
function hideAddForm() {
    $('#addEntrySection').addClass('hidden');
    $('#resultsSection').removeClass('hidden');
    resetForm();
}

// Reset form
function resetForm() {
    $('#addEntryForm')[0].reset();
}

// Show edit form
function showEditForm(entryId) {
    // Fetch the entry data
    $.ajax({
        url: `${API_BASE_URL}/${entryId}`,
        method: 'GET',
        success: function(data) {
            if (data.length > 0) {
                const entry = data[0];
                const person = entry.person || {};
                const address = entry.address || {};

                // Populate the form with entry data
                $('#editEntryId').val(entry.entryId);
                $('#editFirstName').val(person.firstName || '');
                $('#editLastName').val(person.lastName || '');
                $('#editAge').val(person.age || '');
                $('#editGender').val(person.gender || '');
                $('#editMaritalStatus').val(person.maritalStatus || '');
                $('#editStreet').val(address.street || '');
                $('#editCity').val(address.city || '');
                $('#editState').val(address.state || '');
                $('#editZip').val(address.zip || '');
                $('#editEmail').val(address.email || '');
                $('#editPhone').val(address.phone || '');
                $('#editNotes').val(entry.notes || '');

                // Show edit section, hide others
                $('#editEntrySection').removeClass('hidden');
                $('#addEntrySection').addClass('hidden');
                $('#resultsSection').addClass('hidden');

                // Scroll to the edit section
                $('#editEntrySection')[0].scrollIntoView({ behavior: 'smooth', block: 'start' });

                // Set focus to the first editable field (after a small delay to ensure scrolling completes)
                setTimeout(function() {
                    $('#editFirstName').focus();
                }, SCROLL_ANIMATION_DELAY);
            } else {
                showMessage('Entry not found', 'error');
            }
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error loading entry: ' + error);
            showMessage(errorMsg, 'error');
        }
    });
}

// Hide edit form
function hideEditForm() {
    $('#editEntrySection').addClass('hidden');
    $('#resultsSection').removeClass('hidden');
    resetEditForm();
}

// Reset edit form
function resetEditForm() {
    $('#editEntryForm')[0].reset();
}

// Save edited entry (delete old, save new)
function saveEditedEntry() {
    // Get and validate the entry ID
    const entryIdValue = $('#editEntryId').val();
    const entryIdNum = parseInt(entryIdValue);

    if (!entryIdValue || isNaN(entryIdNum) || entryIdNum < 1 || entryIdNum > MAX_ENTRY_ID) {
        showMessage(`Entry ID must be an integer between 1 and ${MAX_ENTRY_ID.toLocaleString()}`, 'error');
        return;
    }

    // Build the updated entry object
    const entry = {
        entryId: entryIdNum,
        person: {
            firstName: $('#editFirstName').val().trim(),
            lastName: $('#editLastName').val().trim(),
            age: $('#editAge').val() ? parseInt($('#editAge').val()) : null,
            gender: $('#editGender').val() || null,
            maritalStatus: $('#editMaritalStatus').val() || null
        },
        address: {
            street: $('#editStreet').val().trim() || null,
            city: $('#editCity').val().trim() || null,
            state: $('#editState').val().trim() || null,
            zip: $('#editZip').val().trim() || null,
            email: $('#editEmail').val().trim() || null,
            phone: $('#editPhone').val().trim() || null
        },
        notes: $('#editNotes').val().trim() || null
    };

    // First, delete the old entry
    $.ajax({
        url: `${API_BASE_URL}/${entryIdNum}`,
        method: 'DELETE',
        success: function() {
            // Then, save the new entry
            $.ajax({
                url: `${API_BASE_URL}/save`,
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(entry),
                success: function() {
                    showMessage('Entry updated successfully!', 'success');
                    hideEditForm();
                    loadAllEntries(false);
                },
                error: function(xhr, status, error) {
                    const errorMsg = getErrorMessage(xhr, 'Error saving updated entry: ' + error);
                    showMessage(errorMsg, 'error');
                }
            });
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error deleting old entry: ' + error);
            showMessage(errorMsg, 'error');
        }
    });
}

// Show message
function showMessage(message, type) {
    const messageDiv = $('#messageSection');
    const messageText = $('#messageText');
    messageDiv.removeClass('hidden success error info');
    messageDiv.addClass(type);
    messageText.text(message);
    // Message stays visible until manually dismissed - no auto-hide
}

// Load all entries (sorted by ID by default)
function loadAllEntries(showFoundMessage) {
    showFoundMessage = showFoundMessage !== false; // default to true
    showLoading();
    $.ajax({
        url: `${API_BASE_URL}/sortById`,
        method: 'GET',
        success: function(data) {
            displayResults(data);
            if (showFoundMessage) {
                showMessage(`Found ${data.length} entries`, 'success');
            }
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error loading entries: ' + error);
            showMessage(errorMsg, 'error');
            $('#results').empty();
        }
    });
}

// Save new entry
function saveEntry(keepOpen) {
    keepOpen = keepOpen || false;

    // Validate entry ID
    const entryIdValue = $('#entryId').val();
    const entryIdNum = parseInt(entryIdValue);

    if (!entryIdValue || isNaN(entryIdNum) || entryIdNum < 1 || entryIdNum > MAX_ENTRY_ID) {
        showMessage(`Entry ID must be an integer between 1 and ${MAX_ENTRY_ID.toLocaleString()}`, 'error');
        return;
    }

    const entry = {
        entryId: entryIdNum,
        person: {
            firstName: $('#firstName').val().trim(),
            lastName: $('#lastName').val().trim(),
            age: $('#age').val() ? parseInt($('#age').val()) : null,
            gender: $('#gender').val() || null,
            maritalStatus: $('#maritalStatus').val() || null
        },
        address: {
            street: $('#street').val().trim() || null,
            city: $('#city').val().trim() || null,
            state: $('#state').val().trim() || null,
            zip: $('#zip').val().trim() || null,
            email: $('#email').val().trim() || null,
            phone: $('#phone').val().trim() || null
        },
        notes: $('#notes').val().trim() || null
    };

    $.ajax({
        url: `${API_BASE_URL}/save`,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(entry),
        success: function() {
            showMessage('Entry saved successfully!', 'success');

            // Update entries list in background
            $.ajax({
                url: API_BASE_URL,
                method: 'GET',
                success: function(data) {
                    // Update loadedEntryIds for next ID calculation
                    loadedEntryIds = data
                        .map(entry => entry.entryId)
                        .filter(id => id !== null && id !== undefined)
                        .sort((a, b) => a - b);
                },
                error: function(xhr, status, error) {
                    console.warn('Failed to update entry IDs in background:', error);
                }
            });

            resetForm();

            if (keepOpen) {
                // Calculate and set next ID
                let nextId = entryIdNum + 1;
                if (loadedEntryIds.length > 0) {
                    const maxId = Math.max(...loadedEntryIds, entryIdNum);
                    nextId = maxId + 1;
                }
                $('#entryId').val(nextId);

                // Keep form open, scroll to top
                window.scrollTo({ top: 0, behavior: 'smooth' });
            } else {
                hideAddForm();
                // Reload all entries to show in results
                loadAllEntries(false);
            }
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error saving entry: ' + error);
            showMessage(errorMsg, 'error');
        }
    });
}

// Delete entry by ID
function deleteEntryById(entryId) {
    $.ajax({
        url: `${API_BASE_URL}/${entryId}`,
        method: 'DELETE',
        success: function() {
            showMessage(`Successfully deleted entries with ID: ${entryId}`, 'success');
            $('#selectedEntryId').val('');
            // Reload all entries to reflect the deletion
            loadAllEntries(false);
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error deleting entry: ' + error);
            showMessage(errorMsg, 'error');
        }
    });
}

// Sort entries by ID
function sortEntriesById() {
    showLoading();
    $.ajax({
        url: `${API_BASE_URL}/sortById`,
        method: 'GET',
        success: function(data) {
            displayResults(data);
            showMessage(`Sorted ${data.length} entries by ID`, 'success');
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error sorting entries: ' + error);
            showMessage(errorMsg, 'error');
            $('#results').empty();
        }
    });
}

// Sort entries by last name
function sortEntriesByLastName() {
    showLoading();
    $.ajax({
        url: `${API_BASE_URL}/sortByLastName`,
        method: 'GET',
        success: function(data) {
            displayResults(data);
            showMessage(`Sorted ${data.length} entries by Last Name`, 'success');
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error sorting entries: ' + error);
            showMessage(errorMsg, 'error');
            $('#results').empty();
        }
    });
}

// Export entries to file
function exportEntries() {
    const fileName = prompt('Enter filename for export:', 'export-data.json');

    // User cancelled the prompt
    if (fileName === null) {
        return;
    }

    // Trim and validate
    const trimmedFileName = fileName.trim();
    if (trimmedFileName === '') {
        showMessage('Filename cannot be empty', 'error');
        return;
    }

    // Check if filename starts with /
    if (trimmedFileName.startsWith('/')) {
        showMessage('Filename cannot begin with /', 'error');
        return;
    }

    // Check if filename contains :
    if (trimmedFileName.includes(':')) {
        showMessage('Filename cannot contain :', 'error');
        return;
    }

    // Check if filename ends with .json
    if (!trimmedFileName.endsWith('.json')) {
        showMessage('Filename must end with .json', 'error');
        return;
    }

    $.ajax({
        url: `${API_BASE_URL}/export?fileName=${encodeURIComponent(trimmedFileName)}`,
        method: 'POST',
        success: function() {
            showMessage(`Entries exported successfully to ${trimmedFileName}`, 'success');
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error exporting entries: ' + error);
            showMessage(errorMsg, 'error');
        }
    });
}

// Import entries from file
function importEntries() {
    const fileName = prompt('Enter filename for import:', 'import-data.json');

    // User cancelled the prompt
    if (fileName === null) {
        return;
    }

    // Trim and validate
    const trimmedFileName = fileName.trim();
    if (trimmedFileName === '') {
        showMessage('Filename cannot be empty', 'error');
        return;
    }

    // Check if filename starts with /
    if (trimmedFileName.startsWith('/')) {
        showMessage('Filename cannot begin with /', 'error');
        return;
    }

    // Check if filename contains :
    if (trimmedFileName.includes(':')) {
        showMessage('Filename cannot contain :', 'error');
        return;
    }

    // Check if filename ends with .json
    if (!trimmedFileName.endsWith('.json')) {
        showMessage('Filename must end with .json', 'error');
        return;
    }

    $.ajax({
        url: `${API_BASE_URL}/importData?fileName=${encodeURIComponent(trimmedFileName)}`,
        method: 'POST',
        success: function() {
            showMessage(`Entries imported successfully from ${trimmedFileName}`, 'success');
            // Reload all entries to show the imported data
            loadAllEntries(false);
        },
        error: function(xhr, status, error) {
            const errorMsg = getErrorMessage(xhr, 'Error importing entries: ' + error);
            showMessage(errorMsg, 'error');
        }
    });
}

$(document).ready(function() {

    // Button click handlers
    $('#listAllBtn').click(function() {
        loadAllEntries();
    });

    $('#addNewBtn').click(function() {
        showAddForm();
    });

    $('#exportBtn').click(function() {
        exportEntries();
    });

    $('#importBtn').click(function() {
        importEntries();
    });

    $('#cancelAddBtn').click(function() {
        hideAddForm();
    });

    $('#saveAndAddAnotherBtn').click(function() {
        saveEntry(true); // true = keep form open for another entry
    });

    $('#cancelEditBtn').click(function() {
        hideEditForm();
    });

    $('#editEntryBtn').click(function() {
        const entryId = $('#selectedEntryId').val().trim();
        if (entryId) {
            const entryIdNum = parseInt(entryId);
            showEditForm(entryIdNum);
        } else {
            showMessage('Please select an entry to edit', 'error');
        }
    });

    // Message close button handler - using event delegation for reliability
    $(document).on('click', '#messageClose', function(e) {
        e.preventDefault();
        e.stopPropagation();
        const messageDiv = $('#messageSection');
        messageDiv.removeClass('success error info');
        $('#messageText').text('');
    });

    // Entry card click handler - populate selected field
    $(document).on('click', '.entry-card', function(e) {
        const entryId = $(this).data('entry-id');
        if (entryId) {
            $('#selectedEntryId').val(entryId);
            // Scroll to selected section
            $('#selectedSection')[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
            // Highlight the input briefly
            $('#selectedEntryId').addClass('highlight');
            setTimeout(function() {
                $('#selectedEntryId').removeClass('highlight');
            }, HIGHLIGHT_DURATION);
        }
    });

    // Search handlers
    $('#searchEntryIdBtn').click(function() {
        const entryId = $('#searchEntryId').val().trim();
        if (entryId) {
            searchByEntryId(entryId);
        } else {
            showMessage('Please enter an entry ID', 'error');
        }
    });

    $('#searchLastNameBtn').click(function() {
        const lastName = $('#searchLastName').val().trim();
        if (lastName) {
            searchByLastName(lastName);
        } else {
            showMessage('Please enter a last name', 'error');
        }
    });

    $('#searchFullNameBtn').click(function() {
        const firstName = $('#searchFirstName').val().trim();
        const lastName = $('#searchFullLastName').val().trim();
        if (firstName && lastName) {
            searchByFullName(firstName, lastName);
        } else {
            showMessage('Please enter both first and last name', 'error');
        }
    });

    // Delete handler
    $('#deleteEntryIdBtn').click(function() {
        const entryId = $('#selectedEntryId').val().trim();
        if (entryId) {
            const entryIdNum = parseInt(entryId);
            const entryName = nameMap.get(entryIdNum) || 'Unknown';
            if (confirm(`Are you sure you want to delete the entry with ID: ${entryId}?\nName: ${entryName}`)) {
                deleteEntryById(entryIdNum);
            }
        } else {
            showMessage('Please select an entry ID to delete', 'error');
        }
    });

    // Sort button handlers
    $('#sortByIdBtn').click(function() {
        sortEntriesById();
    });

    $('#sortByLastNameBtn').click(function() {
        sortEntriesByLastName();
    });

    // Form submission
    $('#addEntryForm').submit(function(e) {
        e.preventDefault();
        saveEntry(false); // false = close form after save
    });

    $('#editEntryForm').submit(function(e) {
        e.preventDefault();
        saveEditedEntry();
    });

    // Initialize: Load all entries on page load
    loadAllEntries();
});

})(); // End of IIFE
