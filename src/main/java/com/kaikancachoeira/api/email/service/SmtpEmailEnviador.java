package com.kaikancachoeira.api.email.service;

import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailEnviador implements EmailEnviador {

    private final JavaMailSender javaMailSender;
    private final EmailProperties properties;

    public SmtpEmailEnviador(JavaMailSender javaMailSender, EmailProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
    }

    @Override
    public void enviar(EmailMensagem mensagem) {
        try {
            MimeMessage mime = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(properties.getRemetente());
            helper.setTo(mensagem.destinatario());
            helper.setSubject(mensagem.assunto());
            helper.setText(mensagem.corpoHtml(), true);
            javaMailSender.send(mime);
        } catch (MessagingException | MailAuthenticationException | MailParseException e) {
            throw new EmailException("Falha de configuração ou de montagem do email SMTP", e, false);
        } catch (MailException e) {
            throw new EmailException("Falha ao enviar email via SMTP", e, true);
        }
    }

}
