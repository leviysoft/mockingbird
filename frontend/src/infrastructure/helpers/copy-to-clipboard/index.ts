import i18n from 'src/mockingbird/i18n';

export default function copyToClipboard(
  text: string,
  callback: (error: Error) => void
) {
  if (text == null) throw new Error(i18n.t('components.copy.textEmptyError'));
  const el = window.document.createElement('textarea');
  el.readOnly = true; // подавляем экранную клавиатуру на touch-устройствах
  Object.assign(
    // важно, чтобы поле было незаменто, но находилось во viewport'е,
    // иначе может измениться позиция скролла (например, в IE)
    el.style,
    {
      width: 1,
      height: 1,
      position: 'fixed',
      top: 0,
      left: 0,
      border: 0,
      padding: 0,
      margin: 0,
      backgroundColor: 'transparent',
      color: 'transparent',
      overflow: 'hidden',
    }
  );
  el.value = text;
  window.document.body.appendChild(el);
  let eventFired = false;
  el.addEventListener('copy', () => {
    eventFired = true;
  });
  try {
    el.select(); // для большинства
    el.setSelectionRange(0, text.length); // для iOS
    global.document.execCommand('copy');
    if (!eventFired) throw new Error(i18n.t('components.copy.copyError'));
    if (callback) callback();
  } catch (e) {
    if (callback) callback(e);
  } finally {
    global.document.body.removeChild(el);
  }
}
