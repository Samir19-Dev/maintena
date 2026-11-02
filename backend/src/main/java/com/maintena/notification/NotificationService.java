package com.maintena.notification;

import com.maintena.notification.event.OrderAssignedEvent;
import com.maintena.notification.event.ReportResolvedEvent;
import com.maintena.user.entity.User;

public interface NotificationService {
	void sendPasswordReset(User user, String rawToken);

	void notifyOrderAssigned(OrderAssignedEvent event);

	void notifyReportResolved(ReportResolvedEvent event);
}
