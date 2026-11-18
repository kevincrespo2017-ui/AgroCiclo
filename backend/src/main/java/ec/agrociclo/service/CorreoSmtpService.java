package ec.agrociclo.service;

import ec.agrociclo.exception.ErrorApi;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
/**
 * CorreoSmtpService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class CorreoSmtpService implements Correo {
    private final JavaMailSender mail;
    private final Environment env;
    public CorreoSmtpService(JavaMailSender mail, Environment env) { this.mail=mail; this.env=env; }
    public void enlace(String destino, String tipo, String token) {
        String titulo = tipo.equals("VERIFICAR") ? "Verifica tu correo" : "Restablece tu contraseña";
        String ruta = tipo.equals("VERIFICAR") ? "verificar" : "restablecer";
        String base = env.getProperty("FRONTEND_URL", "http://localhost:4200").replaceAll("/+$", "");
        String url = base + "/cuenta/" + ruta + "#token=" + token;
        try {
            var mensaje=mail.createMimeMessage();
            var helper=new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(env.getProperty("MAIL_FROM", "AgroCiclo <no-reply@agrociclo.test>"));
            helper.setTo(destino); helper.setSubject(titulo + " | AgroCiclo");
            String seguro=HtmlUtils.htmlEscape(url);
            helper.setText(titulo + ": " + url + " . El enlace vence en 30 minutos y solo puede usarse una vez.",
                "<html><body style='background:#eef3ed;padding:32px;font-family:Arial;color:#163b2b'><div style='max-width:540px;margin:auto;background:white;border-radius:20px;padding:36px'><p style='letter-spacing:3px'>AGROCICLO</p><h1>" + titulo + "</h1><p>Tu campo, conectado. Continúa de forma segura con tu cuenta.</p><p style='padding:24px 0'><a style='background:#215b3d;color:white;padding:16px 24px;border-radius:8px' href='" + seguro + "'>" + titulo + "</a></p><p>El enlace vence en 30 minutos y solo puede usarse una vez.</p><p>Si no solicitaste esta acción, ignora este mensaje.</p></div></body></html>");
            mail.send(mensaje);
        } catch (Exception e) { throw new ErrorApi(503,"CORREO_NO_DISPONIBLE","No se pudo enviar el correo. Intenta más tarde o contacta al administrador."); }
    }

    @Override
    public void codigo(String destino, String codigo) {
        try {
            var mensaje = mail.createMimeMessage();
            var helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(env.getProperty("MAIL_FROM", "AgroCiclo <no-reply@agrociclo.test>"));
            helper.setTo(destino);
            helper.setSubject("Tu código de verificación | AgroCiclo");
            String cuerpo = "<html><body style='background:#eef3ed;padding:32px;font-family:Arial;color:#163b2b'><div style='max-width:540px;margin:auto;background:white;border-radius:20px;padding:36px'><p style='letter-spacing:3px'>AGROCICLO</p><h1>Confirma tu correo</h1><p>Ingresa este código de cuatro dígitos para verificar tu dirección de correo.</p><div style='font-size:36px;font-weight:800;letter-spacing:12px;background:#eff5ed;padding:22px;border-radius:12px;text-align:center'>" + codigo + "</div><p>El código vence en 10 minutos. Si no solicitaste esta cuenta, ignora este mensaje.</p></div></body></html>";
            helper.setText("Tu código de verificación de AgroCiclo es: " + codigo + ". Vence en 10 minutos.", cuerpo);
            mail.send(mensaje);
        } catch (Exception e) {
            throw new ErrorApi(503, "CORREO_NO_DISPONIBLE", "No se pudo enviar el código. Revisa la configuración SMTP o solicita un nuevo código más tarde.");
        }
    }


    @Override
    public void estadoCuenta(String destino, boolean aprobado, String motivo) {
        String titulo = aprobado ? "Tu cuenta fue aprobada" : "Actualización de tu solicitud de acceso";
        String detalle = aprobado ? "Ya puedes iniciar sesión en AgroCiclo con tu correo y contraseña." :
            (motivo == null || motivo.isBlank() ? "El administrador no aprobó tu solicitud. Contacta con la administración para más información." : "Motivo indicado: " + motivo);
        try {
            var mensaje = mail.createMimeMessage();
            var helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(env.getProperty("MAIL_FROM", "AgroCiclo <no-reply@agrociclo.test>"));
            helper.setTo(destino);
            helper.setSubject(titulo + " | AgroCiclo");
            String seguro = HtmlUtils.htmlEscape(detalle);
            helper.setText(titulo + ". " + detalle,
                "<html><body style='background:#eef3ed;padding:32px;font-family:Arial;color:#163b2b'><div style='max-width:540px;margin:auto;background:white;border-radius:20px;padding:36px'><p style='letter-spacing:3px'>AGROCICLO</p><h1>" + titulo + "</h1><p>" + seguro + "</p><p>Gracias por utilizar AgroCiclo.</p></div></body></html>");
            mail.send(mensaje);
        } catch (Exception e) {
            throw new ErrorApi(503, "CORREO_NO_DISPONIBLE", "No se pudo enviar la notificación por correo.");
        }
    }

}
