package com.intellicentrics.choreography.notification.util;

import com.google.common.collect.Maps;
import com.intellicentrics.choreography.notification.email.EsbEmailClient;
import com.intellicentrics.choreography.notification.messaging.enums.EmailMessageType;
import com.intellicentrics.choreography.notification.model.EmailProps;
import com.intellicentrics.choreography.notification.model.external.jportal.User;
import com.intellicentrics.choreography.notification.model.external.reptrax.UserProfile;
import com.intellicentrics.choreography.notification.model.jpa.AppointmentEntity;
import com.intellicentrics.choreography.notification.model.jpa.AppointmentParticipantEntity;
import com.intellicentrics.choreography.notification.model.jpa.CalendarEntity;
import com.intellicentrics.choreography.notification.sync.service.common.JPortalService;
import com.intellicentrics.choreography.notification.sync.service.common.ReptraxUserProfileService;
import com.intellicentrics.common.base.exception.OperationFailedException;
import com.intellicentrics.common.eventdefinition.model.v1.calendar.AppointmentParticipantV1;
import com.intellicentrics.common.eventdefinition.model.v1.calendar.AppointmentStatus;
import com.intellicentrics.common.eventdefinition.model.v1.calendar.AppointmentV1;
import com.intellicentrics.common.eventdefinition.model.v1.group.GroupMemberExternalIdType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static com.intellicentrics.common.base.DateTimeFacade.UTC_DATE_FORMAT;
import static com.intellicentrics.common.eventdefinition.model.v1.calendar.AppointmentType.ACCESS_APPROVAL;

@Component
public class EmailSenderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderUtil.class);
    private final String EMAIL = "EMAIL_ADDRESS";
    private static final String LOCATION_EXTERNAL_ID_TYPE = "VIRTUAL_MEETING_URL";

    @Autowired
    protected EsbEmailClient esbEmailClient;

    @Autowired
    private JPortalService jPortalService;

    @Autowired
    private ReptraxUserProfileService reptraxUserProfileService;

    private NameParser nameParser = new NameParser();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat stf = new SimpleDateFormat("hh:mm aa");
    public final String VIRTUAL = "VIRTUAL";

        public void sendEmailInvitations(AppointmentEntity appointment, boolean isStatusUpdate, boolean isCommentAdded) {
            Collection<AppointmentParticipantEntity> participants = appointment.getParticipants();
            EmailProps emailProps = null;
            for (AppointmentParticipantEntity participant : participants) {
                try {
                    emailProps = new EmailProps();

                    sdf.setTimeZone(TimeZone.getTimeZone(appointment.getCalendar().getTimeZoneId()));
                    stf.setTimeZone(TimeZone.getTimeZone(appointment.getCalendar().getTimeZoneId()));

                    AppointmentParticipantV1 participantV1 = new AppointmentParticipantV1();
                    participantV1.setParticipantExternalId(participant.getParticipantExternalId());
                    participantV1.setParticipantExternalIdType(participant.getParticipantExternalIdType());

                    LOGGER.info("Findind email for user: " + participant.getParticipantName() + " " + participant.getParticipantExternalId() + " " + participant.getParticipantExternalIdType());
                    String userEmail = findEmail(participantV1);
                    if(!StringUtils.isNotEmpty(userEmail)) {
                    	LOGGER.warn("Email not found for user: "+ participant.getParticipantName() + " " + participant.getParticipantExternalId() + " " + participant.getParticipantExternalIdType());
                    	continue;
                    }
                    LOGGER.info("Email found for user: " + participant.getParticipantName() + " " + emailProps.getEmailAddress());
                    emailProps.setParticipantName(participant.getParticipantName());

                    if (appointment.getDateTime() != null) {
                        emailProps.setEventDate(sdf.format(appointment.getDateTime()));
                        emailProps.setEventTime(stf.format(appointment.getDateTime()) + " " + appointment.getCalendar().getTimeZoneId());
                    }
                    emailProps.setFacilityName(appointment.getLocationName());
                    emailProps.setUserId(participant.getId());
                    emailProps.setEventType(appointment.getType());
                    emailProps.setEventName(appointment.getTitle());
                    emailProps.setStatus(appointment.getStatus().name());

                    if (!CollectionUtils.isEmpty(appointment.getNotes())) {
                        emailProps.setNewComment(appointment.getNotes().stream().findFirst().get().getSenderName() + ": " + appointment.getNotes().stream().findFirst().get().getNote());
                        emailProps.setCommentHistory(appointment.getNotes().stream().map(appointmentNoteV1 ->
                                appointmentNoteV1.getSenderName() + ": " + appointmentNoteV1.getNote()).collect(Collectors.joining("<br>")));
                    }

                    if (!CollectionUtils.isEmpty(appointment.getParticipants())) {
                        emailProps.setParticipants(
                                appointment.getParticipants().stream().filter(appointmentParticipantV1 -> !participant.getParticipantName()
                                        .equals(appointmentParticipantV1.getParticipantName()))
                                        .map(appointmentParticipantV1 -> appointmentParticipantV1.getParticipantName()).collect(Collectors.joining("<br>"))
                        );
                    }

                    emailProps.setMessageType(EmailMessageType.EMAIL_EVENT_UPDATED_INVITATION_NOTIFICATION);
                    if (Objects.nonNull(emailProps.getEmailAddress()) && StringUtils.isNotEmpty(emailProps.getEmailAddress())) {
                        boolean isInPersonMeetingRequest = appointment.getType().equals(ACCESS_APPROVAL);
                        boolean isVirtualMeetingRequest = appointment.getLocationExternalIdType().equals(LOCATION_EXTERNAL_ID_TYPE);
                        if (!participant.getPrimary() && (isInPersonMeetingRequest)) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_MEETING_REQUEST_STATUS_UPDATE_NOTIFICATION);
                        } else if (isVirtualMeetingRequest && isInPersonMeetingRequest) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_VIRTUAL_MEETING_REQUEST_STATUS_UPDATE_NOTIFICATION);
                            emailProps.setEventType("Virtual");
                        } else if (isVirtualMeetingRequest && !isInPersonMeetingRequest) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_IN_PERSON_MEETING_REQUEST_STATUS_UPDATE_NOTIFICATION);
                            emailProps.setEventType("In Person");
                        }
                        if ((isVirtualMeetingRequest || isInPersonMeetingRequest) && isCommentAdded) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_MEETING_REQUEST_COMMENT_ADDED_NOTIFICATION);
                        }

                        if (EmailMessageType.EMAIL_EVENT_UPDATED_INVITATION_NOTIFICATION.equals(emailProps.getMessageType()) &&
                                "HCP".equalsIgnoreCase(participant.getParticipantRole())) {
                            return;
                        }
                        sendInvitationEmail(emailProps);
                        //sendInvitationEmail(emailAddress, participantName, userId, eventType, eventDate, eventTime, facilityName, eventName, newComment, commentHistory, type, status, participantList);
                    }
                } catch (Throwable t) {
                    LOGGER.warn("Exception in external integration for sending invitations", t);
                }
            }
        }

        public void sendEmailInvitations(AppointmentV1 appointment, CalendarEntity calendarEntity) {
        	if (!CollectionUtils.isEmpty(appointment.getParticipants())) {
                Collection<AppointmentParticipantV1> participants = appointment.getParticipants();
                for (AppointmentParticipantV1 participant : participants) {
                        try {
                                //Get Email and send notification
                                LOGGER.info("Findind email for user: " + participant.getParticipantName() + " " + participant.getParticipantExternalId() + " " + participant.getParticipantExternalIdType());
                                String email = findEmail(participant);
                                LOGGER.info("Email found for user: " + participant.getParticipantName() + " " + email);
                                if(StringUtils.isNotEmpty(email)){
                                        sendInvitation(appointment, participant, email, calendarEntity);
                                }
                        } catch (OperationFailedException e) {
                                LOGGER.error("Error in adding recipient", e);
                        }
                }
        	}
        }

        public void sendCalendarApproverEmailInvitation(AppointmentV1 appointment, CalendarEntity calendarEntity, String calendarApproverId, String calendarApproverIdType){
                AppointmentParticipantV1 participant = new AppointmentParticipantV1();
                participant.setId(calendarApproverId);
                participant.setParticipantExternalId(calendarApproverId);
                participant.setParticipantExternalIdType(calendarApproverIdType);
                participant.setPrimary(false);
                Map<String, String> userInfo = findUserInfo(participant);
                String email = userInfo.get("email");
                String name = userInfo.get("name");
                participant.setParticipantName(name);
                sendInvitation(appointment, participant, email, calendarEntity);
        }

        private Boolean sendInvitation(AppointmentV1 appointment, AppointmentParticipantV1 participant, String emailAddress, CalendarEntity calendarEntity) throws OperationFailedException {
            EmailProps emailProps = new EmailProps();

            emailProps.setParticipantName(participant.getParticipantName());
            emailProps.setFacilitySchedulerName(appointment.getContactName());
            emailProps.setFacilitySchedulerEmail(appointment.getContactEmailAddress());

            NameParser.NameDetails nameDetails = nameParser.parse(emailProps.getParticipantName());
            if (nameDetails.isOnlyNameDetail()) {
                nameDetails.setLastName(nameDetails.getFirstName());
            }
            Boolean sent = Boolean.FALSE;
            try {
                sdf.setTimeZone(TimeZone.getTimeZone(calendarEntity.getTimeZoneId()));
                stf.setTimeZone(TimeZone.getTimeZone(calendarEntity.getTimeZoneId()));
                if (appointment.getDateTime() != null) {
                    org.joda.time.DateTime dateTime = parseDate(appointment.getDateTime());
                    emailProps.setEventDate(sdf.format(dateTime.toDate()));
                    emailProps.setEventTime(stf.format(dateTime.toDate()) + " " + calendarEntity.getTimeZoneId());
                }
                emailProps.setFacilityName(appointment.getLocationName());
                emailProps.setUserId(participant.getId());
                emailProps.setEventName(appointment.getTitle());
                emailProps.setStatus(appointment.getStatus());
                if (!CollectionUtils.isEmpty(appointment.getNotes())) {
                    emailProps.setNewComment(appointment.getNotes().get(appointment.getNotes().size() - 1).getNote());
                    emailProps.setCommentHistory(appointment.getNotes().stream().map(appointmentNoteV1 ->
                            appointmentNoteV1.getSenderName() + ": " + appointmentNoteV1.getNote()).collect(Collectors.joining("<br>")));
                }

                if (!CollectionUtils.isEmpty(appointment.getParticipants())) {
                    emailProps.setParticipants(
                            appointment.getParticipants().stream().filter(appointmentParticipantV1 -> !participant.getParticipantName()
                                    .equals(appointmentParticipantV1.getParticipantName()))
                                    .map(appointmentParticipantV1 -> appointmentParticipantV1.getParticipantName()).collect(Collectors.joining("<br>"))
                    );
                }

                boolean isAccessApprovalMeetingRequest = appointment.getType().equals(ACCESS_APPROVAL.name());
                boolean isVirtualMeetingRequest = appointment.getLocationExternalIdType().equals(LOCATION_EXTERNAL_ID_TYPE);
                emailProps.setMessageType(EmailMessageType.EMAIL_EVENT_INVITATION_NOTIFICATION);
                emailProps.setEventType(appointment.getType());

                if (StringUtils.isNotEmpty(emailAddress)) {
                    if (participant.getPrimary()) {
                        if (isAccessApprovalMeetingRequest && !isVirtualMeetingRequest) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_IN_PERSON_MEETING_REQUEST_REQUESTER_CONFIRMATION);
                            emailProps.setEventType("In Person");
                        } else if (isAccessApprovalMeetingRequest && isVirtualMeetingRequest) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_VIRTUAL_MEETING_REQUEST_REQUESTER_CONFIRMATION);
                            emailProps.setEventType("Virtual");
                        }
                    } else {
                        if (isAccessApprovalMeetingRequest && !isVirtualMeetingRequest) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_MEETING_REQUEST_STATUS_UPDATE_NOTIFICATION);
                            emailProps.setEventType("In Person");
                        } else if (isAccessApprovalMeetingRequest && isVirtualMeetingRequest) {
                            emailProps.setMessageType(EmailMessageType.EMAIL_MEETING_REQUEST_STATUS_UPDATE_NOTIFICATION);
                            emailProps.setEventType("Virtual");
                        }
                    }
                    if (EmailMessageType.EMAIL_EVENT_INVITATION_NOTIFICATION.equals(emailProps.getMessageType()) &&
                            "HCP".equalsIgnoreCase(participant.getParticipantRole())) {
                        return false;
                    }
                    emailProps.setEmailAddress(emailAddress);
                    sendInvitationEmail(emailProps);
                }
                sent = Boolean.TRUE;
            } catch (Throwable t) {
                LOGGER.warn("Exception in external integration for sending invitations", t);
            }
            return sent;
        }

        public void sendAppointmentReminderEmail(AppointmentV1 appointment, CalendarEntity calendarEntity, AppointmentParticipantV1 participant) {
                try {
                        //Get Email and send notification
                        LOGGER.info("Findind email for user: " + participant.getParticipantName() + " " + participant.getParticipantExternalId() + " "
                                + participant.getParticipantExternalIdType());
                        String email = findEmail(participant);
                        LOGGER.info("Email found for user: " + participant.getParticipantName() + " " + email);
                        if (StringUtils.isNotEmpty(email)) {
                                sendAppointmentReminderEmail(appointment, participant, email, calendarEntity);
                        }
                } catch (OperationFailedException e) {
                        LOGGER.error("Error in adding recipient", e);
                }
        }

        public boolean sendAppointmentReminderEmail(AppointmentV1 appointment, AppointmentParticipantV1 participant, String emailAddress,
                CalendarEntity calendarEntity) {
                Boolean sent = Boolean.FALSE;
                try {
                        String participantName = participant.getParticipantName();
                        NameParser.NameDetails nameDetails = nameParser.parse(participantName);

                        if (nameDetails.isOnlyNameDetail()) {
                                nameDetails.setLastName(nameDetails.getFirstName());
                        }

                        sdf.setTimeZone(TimeZone.getTimeZone(calendarEntity.getTimeZoneId()));
                        stf.setTimeZone(TimeZone.getTimeZone(calendarEntity.getTimeZoneId()));
                        String eventDate = "";
                        String eventTime = "";
                        if (appointment.getDateTime() != null) {
                                org.joda.time.DateTime dateTime = parseDate(appointment.getDateTime());
                                eventDate = sdf.format(dateTime.toDate());
                                eventTime = stf.format(dateTime.toDate()) + " " + calendarEntity.getTimeZoneId();
                        }
                        String facilityName = appointment.getLocationName();
                        EmailMessageType type = EmailMessageType.EMAIL_MEETING_REMINDER_NOTIFICATION;
                        String eventType = appointment.getType();
                        String participants = "";
                        if (Objects.nonNull(appointment.getParticipants()) && appointment.getParticipants().size() > 0) {
                                participants = appointment.getParticipants().stream().filter(appointmentParticipantV1 -> !participant.getParticipantName()
                                        .equals(appointmentParticipantV1.getParticipantName()))
                                        .map(appointmentParticipantV1 -> appointmentParticipantV1.getParticipantName()).collect(Collectors.joining(", "));
                        }

                        if (Objects.nonNull(emailAddress) && StringUtils.isNotEmpty(emailAddress)) {
                                Map<String, Object> templateProperties = Maps.newHashMap();
                                templateProperties.put(EsbEmailClient.TO_EMAIL, emailAddress);
                                templateProperties.put("givenName", nameDetails.getFirstName());
                                templateProperties.put("eventDate", eventDate);
                                templateProperties.put("eventTime", eventTime);
                                templateProperties.put("facility", facilityName);
                                templateProperties.put("participants", participants);

                                sent = esbEmailClient.sendEmail(type, templateProperties);
                        }

                        sent = Boolean.TRUE;
                } catch (Throwable t) {
                        LOGGER.warn("Exception in external integration for sending invitations", t);
                }
                return sent;
        }

        private static org.joda.time.DateTime parseDate(String value) {
                return UTC_DATE_FORMAT.withZoneUTC().parseDateTime(value);
        }

        private Boolean sendInvitationEmail(EmailProps emailProps) throws OperationFailedException {
            NameParser.NameDetails nameDetails = nameParser.parse(emailProps.getParticipantName());
            Map<String, Object> templateProperties = Maps.newHashMap();
            templateProperties.put(EsbEmailClient.TO_EMAIL, emailProps.getEmailAddress());
            templateProperties.put("givenName", nameDetails.getFirstName());
            templateProperties.put("userId", "");
            templateProperties.put("eventType", emailProps.getEventType());
            templateProperties.put("eventDate", emailProps.getEventDate());
            templateProperties.put("eventTime", emailProps.getEventTime());
            templateProperties.put("eventName", emailProps.getEventName());
            templateProperties.put("facility", emailProps.getFacilityName());
            templateProperties.put("participants", emailProps.getParticipants());
            templateProperties.put("newComment", emailProps.getNewComment() != null ? emailProps.getNewComment() : "");
            templateProperties.put("commentHistory", emailProps.getCommentHistory() != null ? emailProps.getCommentHistory(): "");
            templateProperties.put("status", emailProps.getStatus().toLowerCase());
            templateProperties.put("fs_name", emailProps.getFacilitySchedulerName());
            templateProperties.put("fs_emailAddress", emailProps.getFacilitySchedulerEmail());
            if (emailProps.getStatus().equals(AppointmentStatus.ACTIVE.name())) {
                templateProperties.put("status", "approved");
            }

            //company
            Boolean sent = esbEmailClient.sendEmail(emailProps.getMessageType(), templateProperties);
            return sent;
        }

        public String findEmail( AppointmentParticipantV1 participant ){
                String email = new String();
                try {
                        LOGGER.info("Retrieving email for " + participant.getParticipantExternalIdType());
                        if(participant.getParticipantExternalIdType().equals(GroupMemberExternalIdType.HCIR_ID.name())){
                                Optional<UserProfile> userProfile = reptraxUserProfileService.getUserProfileByUserId(participant.getParticipantExternalId());
                                email = !userProfile.get().getEmail().isEmpty() ? userProfile.get().getEmail(): "";
                        }
                        if(participant.getParticipantExternalIdType().equals(GroupMemberExternalIdType.INDIVIDUAL_ID.name())){
                                User user = jPortalService.getIndividualEmail(participant.getParticipantExternalId());
                                email = !user.getEmail().isEmpty() ? user.getEmail() : "";
                        }
                        if(participant.getParticipantExternalIdType().equals(EMAIL)){
                                email = participant.getParticipantExternalId();
                        }
                } catch (Exception e){
                        LOGGER.warn("Error retrieving email ", e);
                }
                LOGGER.info("Email retrieved " + email);
                return email;
        }

        private Map<String, String> findUserInfo( AppointmentParticipantV1 participant ){
                Map<String, String> userInfoResponse = new HashMap<>();
                try {
                        LOGGER.info("Retrieving userinfo for " + participant.getParticipantExternalIdType());
                        if(participant.getParticipantExternalIdType().equals(GroupMemberExternalIdType.HCIR_ID.name())){
                                Optional<UserProfile> userProfile = reptraxUserProfileService.getUserProfileByUserId(participant.getParticipantExternalId());
                                userInfoResponse.put("email", userProfile.get().getEmail());
                                userInfoResponse.put("name", userProfile.get().getFirstName() + " " + userProfile.get().getLastName());
                        }
                        if(participant.getParticipantExternalIdType().equals(GroupMemberExternalIdType.INDIVIDUAL_ID.name())){
                                User user = jPortalService.getIndividualEmail(participant.getParticipantExternalId());
                                userInfoResponse.put("email", user.getEmail());
                                userInfoResponse.put("name", user.getGivenName());
                        }
                        if(participant.getParticipantExternalIdType().equals(EMAIL)){
                                userInfoResponse.put("email", participant.getParticipantExternalId());
                                userInfoResponse.put("name", participant.getParticipantName());
                        }
                } catch (Exception e){
                        LOGGER.warn("Error retrieving email ", e);
                }
                LOGGER.info("Information retrieved " + userInfoResponse);
                return userInfoResponse;
        }
}
