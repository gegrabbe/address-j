$(document).ready(function() {
    const API_BASE_URL = '/api/entries';

    // Helper function to extract error message from response
    function getErrorMessage(xhr, defaultError) {
        if (xhr.responseJSON && xhr.responseJSON.message) {
            return xhr.responseJSON.error + ': ' + xhr.responseJSON.message;
        }
        return defaultError;
    }

    // Button click handlers
    $('#listAllBtn').click(function() {
        loadAllEntries();
    });

    $('#addNewBtn').click(function() {
        showAddForm();
    });

    $('#cancelAddBtn').click(function() {
        hideAddForm();
    });

    // Message close button handler - using event delegation for reliability
    $(document).on('click', '#messageClose', function(e) {
        e.preventDefault();
        e.stopPropagation();
        $('#messageSection').addClass('hidden');
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
        const entryId = $('#deleteEntryId').val().trim();
        if (entryId) {
            if (confirm(`Are you sure you want to delete all entries with ID: ${entryId}?`)) {
                deleteEntryById(entryId);
            }
        } else {
            showMessage('Please enter an entry ID to delete', 'error');
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
        saveEntry();
    });

    // Load all entries
    function loadAllEntries() {
        showLoading();
        $.ajax({
            url: API_BASE_URL,
            method: 'GET',
            success: function(data) {
                displayResults(data);
                showMessage(`Found ${data.length} entries`, 'success');
            },
            error: function(xhr, status, error) {
                const errorMsg = getErrorMessage(xhr, 'Error loading entries: ' + error);
                showMessage(errorMsg, 'error');
                $('#results').empty();
            }
        });
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

    // Save new entry
    function saveEntry() {
        // Validate entry ID
        const entryIdValue = $('#entryId').val();
        const entryIdNum = parseInt(entryIdValue);

        if (!entryIdValue || isNaN(entryIdNum) || entryIdNum < 1 || entryIdNum > 999999) {
            showMessage('Entry ID must be an integer between 1 and 999,999', 'error');
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
                resetForm();
                hideAddForm();
                // Optionally reload all entries
                loadAllEntries();
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
                $('#deleteEntryId').val('');
                // Reload all entries to reflect the deletion
                loadAllEntries();
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

    // Display results
    function displayResults(entries) {
        const resultsDiv = $('#results');
        resultsDiv.empty();

        $('#resultsCount').text(`(${entries.length})`);

        if (entries.length === 0) {
            resultsDiv.html('<p class="loading">No entries found</p>');
            return;
        }

        entries.forEach(function(entry) {
            const card = createEntryCard(entry);
            resultsDiv.append(card);
        });
    }

    // Create entry card HTML
    function createEntryCard(entry) {
        const person = entry.person || {};
        const address = entry.address || {};

        let html = '<div class="entry-card">';
        html += '<div class="entry-header">';
        html += `<div class="entry-id">ID: ${entry.entryId || 'N/A'}</div>`;
        html += `<div class="entry-name">${person.firstName || ''} ${person.lastName || ''}</div>`;
        html += '</div>';

        // Person details
        html += '<div class="entry-section">';
        html += '<div class="entry-section-title">Person Details</div>';
        if (person.age) {
            html += `<div class="entry-detail"><span class="entry-detail-label">Age:</span><span class="entry-detail-value">${person.age}</span></div>`;
        }
        if (person.gender) {
            html += `<div class="entry-detail"><span class="entry-detail-label">Gender:</span><span class="entry-detail-value">${person.gender}</span></div>`;
        }
        if (person.maritalStatus) {
            html += `<div class="entry-detail"><span class="entry-detail-label">Marital Status:</span><span class="entry-detail-value">${person.maritalStatus}</span></div>`;
        }
        html += '</div>';

        // Address details
        if (address.street || address.city || address.state || address.zip || address.email || address.phone) {
            html += '<div class="entry-section">';
            html += '<div class="entry-section-title">Contact Information</div>';
            if (address.street) {
                html += `<div class="entry-detail"><span class="entry-detail-label">Street:</span><span class="entry-detail-value">${address.street}</span></div>`;
            }
            if (address.city || address.state || address.zip) {
                let cityStateZip = [address.city, address.state, address.zip].filter(Boolean).join(', ');
                html += `<div class="entry-detail"><span class="entry-detail-label">Location:</span><span class="entry-detail-value">${cityStateZip}</span></div>`;
            }
            if (address.email) {
                html += `<div class="entry-detail"><span class="entry-detail-label">Email:</span><span class="entry-detail-value">${address.email}</span></div>`;
            }
            if (address.phone) {
                html += `<div class="entry-detail"><span class="entry-detail-label">Phone:</span><span class="entry-detail-value">${address.phone}</span></div>`;
            }
            html += '</div>';
        }

        // Notes
        if (entry.notes) {
            html += '<div class="entry-section">';
            html += '<div class="entry-section-title">Notes</div>';
            html += `<div class="entry-notes">${entry.notes}</div>`;
            html += '</div>';
        }

        html += '</div>';
        return html;
    }

    // Show add form
    function showAddForm() {
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

    // Show loading
    function showLoading() {
        $('#results').html('<p class="loading">Loading...</p>');
        $('#resultsCount').text('');
    }

    // Show message
    function showMessage(message, type) {
        const messageDiv = $('#messageSection');
        const messageText = $('#messageText');
        messageDiv.removeClass('hidden success error info');
        messageDiv.addClass(type);
        messageText.text(message);

        // Auto-hide after 5 seconds for success and info messages only
        // Error messages stay visible until manually dismissed
        if (type === 'success' || type === 'info') {
            setTimeout(function() {
                messageDiv.addClass('hidden');
            }, 5000);
        }
    }

    // Initialize: Load all entries on page load
    loadAllEntries();
});
