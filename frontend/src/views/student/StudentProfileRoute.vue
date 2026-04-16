<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { fetchStudentProfileApi, updateStudentProfileApi, uploadStudentAvatarApi } from '@/api/user'
import type { StudentProfile, UpdateStudentProfileRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const BACKEND_ORIGIN = `${window.location.protocol}//${window.location.hostname}:8080`
const STUDENT_AVATAR_STORAGE_KEY = 'jqpro.student-avatar-url'
const AVATAR_EVENT_NAME = 'jqpro:student-avatar-updated'
const cropViewportSize = 280

const avatarPresets = Array.from({ length: 10 }, (_, index) => ({
  id: index + 1,
  label: `预设头像 ${String(index + 1).padStart(2, '0')}`,
  url: `${BACKEND_ORIGIN}/assets/avatars/presets/avatar-${String(index + 1).padStart(2, '0')}.jpg`
}))

const loading = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)
const errorMessage = ref('')
const profile = ref<StudentProfile | null>(null)
const showPresetPanel = ref(false)
const showCropModal = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const cropSourceUrl = ref('')
const cropNaturalWidth = ref(1)
const cropNaturalHeight = ref(1)
const cropZoom = ref(1)
const cropOffsetX = ref(0)
const cropOffsetY = ref(0)
const isDraggingCrop = ref(false)
const dragStart = reactive({
  pointerX: 0,
  pointerY: 0,
  offsetX: 0,
  offsetY: 0
})

const form = reactive<UpdateStudentProfileRequest>({
  avatarUrl: null,
  college: null,
  grade: null,
  gender: null,
  phone: null,
  emergencyContact: null,
  emergencyPhone: null
})

const currentAvatarUrl = computed(() => form.avatarUrl || avatarPresets[0].url)
const currentPresetId = computed(() => avatarPresets.find((preset) => preset.url === form.avatarUrl)?.id ?? null)
const cropScaleBase = computed(() => Math.max(cropViewportSize / cropNaturalWidth.value, cropViewportSize / cropNaturalHeight.value))
const renderedCropWidth = computed(() => cropNaturalWidth.value * cropScaleBase.value * cropZoom.value)
const renderedCropHeight = computed(() => cropNaturalHeight.value * cropScaleBase.value * cropZoom.value)
const cropImageLeft = computed(() => cropViewportSize / 2 - renderedCropWidth.value / 2 + cropOffsetX.value)
const cropImageTop = computed(() => cropViewportSize / 2 - renderedCropHeight.value / 2 + cropOffsetY.value)
const cropImageStyle = computed(() => ({
  width: `${renderedCropWidth.value}px`,
  height: `${renderedCropHeight.value}px`,
  left: `${cropImageLeft.value}px`,
  top: `${cropImageTop.value}px`
}))

function syncForm(data: StudentProfile): void {
  form.avatarUrl = data.avatarUrl
  form.college = data.college
  form.grade = data.grade
  form.gender = data.gender
  form.phone = data.phone
  form.emergencyContact = data.emergencyContact
  form.emergencyPhone = data.emergencyPhone
}

function cacheStudentAvatar(avatarUrl: string | null | undefined): void {
  if (avatarUrl) {
    localStorage.setItem(STUDENT_AVATAR_STORAGE_KEY, avatarUrl)
  } else {
    localStorage.removeItem(STUDENT_AVATAR_STORAGE_KEY)
  }
  window.dispatchEvent(new Event(AVATAR_EVENT_NAME))
}

async function loadProfile(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await fetchStudentProfileApi()
    profile.value = data
    syncForm(data)
    cacheStudentAvatar(data.avatarUrl)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function saveProfile(): Promise<void> {
  saving.value = true
  errorMessage.value = ''

  try {
    const data = await updateStudentProfileApi({ ...form })
    profile.value = data
    syncForm(data)
    cacheStudentAvatar(data.avatarUrl)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function choosePresetAvatar(avatarUrl: string): void {
  form.avatarUrl = avatarUrl
  showPresetPanel.value = false
}

function openFilePicker(): void {
  fileInputRef.value?.click()
}

async function handleAvatarFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  input.value = ''

  try {
    const sourceUrl = await readFileAsObjectUrl(file)
    await openCropper(sourceUrl)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  }
}

function readFileAsObjectUrl(file: File): Promise<string> {
  return Promise.resolve(URL.createObjectURL(file))
}

async function openCropper(sourceUrl: string): Promise<void> {
  cleanupCropSource()
  const image = await loadImage(sourceUrl)
  cropSourceUrl.value = sourceUrl
  cropNaturalWidth.value = image.naturalWidth || image.width
  cropNaturalHeight.value = image.naturalHeight || image.height
  cropZoom.value = 1
  cropOffsetX.value = 0
  cropOffsetY.value = 0
  showCropModal.value = true
}

function loadImage(sourceUrl: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('图片读取失败，请换一张试试'))
    image.src = sourceUrl
  })
}

function clampCropOffsets(): void {
  const maxX = Math.max(0, (renderedCropWidth.value - cropViewportSize) / 2)
  const maxY = Math.max(0, (renderedCropHeight.value - cropViewportSize) / 2)
  cropOffsetX.value = Math.min(maxX, Math.max(-maxX, cropOffsetX.value))
  cropOffsetY.value = Math.min(maxY, Math.max(-maxY, cropOffsetY.value))
}

function handleCropZoomChange(): void {
  clampCropOffsets()
}

function startCropDrag(event: PointerEvent): void {
  isDraggingCrop.value = true
  dragStart.pointerX = event.clientX
  dragStart.pointerY = event.clientY
  dragStart.offsetX = cropOffsetX.value
  dragStart.offsetY = cropOffsetY.value
  ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}

function handleCropDrag(event: PointerEvent): void {
  if (!isDraggingCrop.value) {
    return
  }
  cropOffsetX.value = dragStart.offsetX + event.clientX - dragStart.pointerX
  cropOffsetY.value = dragStart.offsetY + event.clientY - dragStart.pointerY
  clampCropOffsets()
}

function stopCropDrag(): void {
  isDraggingCrop.value = false
}

function cancelCropper(): void {
  showCropModal.value = false
  cleanupCropSource()
}

function cleanupCropSource(): void {
  if (cropSourceUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(cropSourceUrl.value)
  }
  cropSourceUrl.value = ''
}

async function confirmCroppedAvatar(): Promise<void> {
  if (!cropSourceUrl.value) {
    return
  }
  uploadingAvatar.value = true
  errorMessage.value = ''

  try {
    const blob = await buildCroppedAvatarBlob()
    const file = new File([blob], `avatar-${Date.now()}.jpg`, { type: 'image/jpeg' })
    const response = await uploadStudentAvatarApi(file)
    form.avatarUrl = response.avatarUrl
    showCropModal.value = false
    cleanupCropSource()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    uploadingAvatar.value = false
  }
}

async function buildCroppedAvatarBlob(): Promise<Blob> {
    const image = await loadImage(cropSourceUrl.value)
    const canvas = document.createElement('canvas')
    canvas.width = 720
    canvas.height = 720
    const context = canvas.getContext('2d')
    if (!context) {
      throw new Error('头像裁切失败，请稍后重试')
    }

    const sourceX = Math.max(0, ((0 - cropImageLeft.value) / renderedCropWidth.value) * cropNaturalWidth.value)
    const sourceY = Math.max(0, ((0 - cropImageTop.value) / renderedCropHeight.value) * cropNaturalHeight.value)
    const sourceWidth = (cropViewportSize / renderedCropWidth.value) * cropNaturalWidth.value
    const sourceHeight = (cropViewportSize / renderedCropHeight.value) * cropNaturalHeight.value

    context.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, canvas.width, canvas.height)

    return await new Promise((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('头像生成失败，请重新调整'))
          return
        }
        resolve(blob)
      }, 'image/jpeg', 0.92)
    })
}

onMounted(() => {
  void loadProfile()
})

onBeforeUnmount(() => {
  cleanupCropSource()
})
</script>

<template>
  <main class="profile-page">
    <div class="profile-shell">
      <header class="profile-hero">
        <div>
          <span class="hero-tag">Profile Atelier</span>
          <h1 class="hero-title">{{ profile?.displayName || '个人资料' }}</h1>
          <p class="hero-copy">在这里整理头像、联系方式和紧急联络信息。头像支持系统预设，也支持上传后精细裁切。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在读取个人资料...</p>
      </div>

      <section v-else class="profile-grid">
        <aside class="profile-portrait-panel">
          <div class="portrait-card">
            <div class="portrait-circle">
              <img :src="currentAvatarUrl" alt="头像预览" class="portrait-image" />
            </div>
            <div class="portrait-copy">
              <p class="portrait-kicker">头像设置</p>
              <h2>先选默认头像，或上传并裁切自己的照片。</h2>
              <p>圆形框会保留头像主体，框外区域会被裁掉。你可以拖动位置并控制缩放。</p>
            </div>

            <div class="portrait-actions">
              <button class="primary-btn" type="button" @click="showPresetPanel = !showPresetPanel">
                {{ showPresetPanel ? '收起默认头像' : '选择默认头像' }}
              </button>
              <button class="secondary-btn" type="button" @click="openFilePicker">上传并裁切</button>
            </div>

            <input ref="fileInputRef" type="file" accept="image/*" class="hidden-input" @change="handleAvatarFileChange" />

            <div v-if="showPresetPanel" class="preset-grid">
              <button
                v-for="preset in avatarPresets"
                :key="preset.id"
                class="preset-item"
                :class="{ 'is-active': currentPresetId === preset.id }"
                type="button"
                @click="choosePresetAvatar(preset.url)"
              >
                <img :src="preset.url" :alt="preset.label" />
              </button>
            </div>
          </div>

          <div class="readonly-card">
            <p class="readonly-title">身份信息</p>
            <div class="readonly-row"><span>真实姓名</span><strong>{{ profile?.realName || '未填写' }}</strong></div>
            <div class="readonly-row"><span>学号</span><strong>{{ profile?.studentNo || '未填写' }}</strong></div>
            <div class="readonly-row"><span>系统账号</span><strong>{{ profile?.account || '未填写' }}</strong></div>
            <div class="readonly-row"><span>咨询师编号</span><strong>{{ profile?.counselorUserId || '暂未分配' }}</strong></div>
          </div>
        </aside>

        <section class="profile-form-panel">
          <div class="section-head">
            <div>
              <p class="section-kicker">可编辑资料</p>
              <h2>更新联络与补充信息</h2>
            </div>
            <span class="section-note">保存后立即生效</span>
          </div>

          <div class="form-grid">
            <label class="field field--full">
              <span class="field-label">显示名称</span>
              <input type="text" class="field-input" :value="profile?.displayName || ''" disabled>
            </label>

            <label class="field">
              <span class="field-label">性别</span>
              <input v-model="form.gender" type="text" class="field-input" placeholder="例如：男 / 女 / 其他">
            </label>

            <label class="field">
              <span class="field-label">所属学院</span>
              <input v-model="form.college" type="text" class="field-input" placeholder="例如：软件学院">
            </label>

            <label class="field">
              <span class="field-label">当前年级</span>
              <input v-model="form.grade" type="text" class="field-input" placeholder="例如：2023级">
            </label>

            <label class="field">
              <span class="field-label">手机号</span>
              <input v-model="form.phone" type="text" class="field-input" placeholder="请输入常用手机号">
            </label>

            <label class="field">
              <span class="field-label">紧急联系人</span>
              <input v-model="form.emergencyContact" type="text" class="field-input" placeholder="例如：家长 / 导师">
            </label>

            <label class="field">
              <span class="field-label">紧急联系电话</span>
              <input v-model="form.emergencyPhone" type="text" class="field-input" placeholder="请输入紧急联系电话">
            </label>
          </div>

          <div class="save-bar">
            <button class="save-btn" type="button" :disabled="saving || uploadingAvatar" @click="saveProfile">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
          </div>
        </section>
      </section>

      <div v-if="showCropModal" class="crop-modal" @click.self="cancelCropper">
        <div class="crop-dialog">
          <div class="crop-copy">
            <p class="section-kicker">上传头像</p>
            <h2>拖动位置并缩放，让主体落在圆形取景框内。</h2>
          </div>
          <div
            class="crop-stage"
            @pointerdown="startCropDrag"
            @pointermove="handleCropDrag"
            @pointerup="stopCropDrag"
            @pointerleave="stopCropDrag"
          >
            <img :src="cropSourceUrl" alt="裁切预览" class="crop-image" :style="cropImageStyle" draggable="false" />
            <div class="crop-mask"></div>
            <div class="crop-guide"></div>
          </div>
          <div class="crop-controls">
            <label class="zoom-control">
              <span>缩放</span>
              <input v-model="cropZoom" type="range" min="1" max="3" step="0.01" @input="handleCropZoomChange">
            </label>
          </div>
          <div class="crop-actions">
            <button class="secondary-btn" type="button" @click="cancelCropper">取消</button>
            <button class="primary-btn" type="button" :disabled="uploadingAvatar" @click="confirmCroppedAvatar">
              {{ uploadingAvatar ? '处理中...' : '应用头像' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.profile-page {
  min-height: 100vh;
  padding: 4rem 2rem 6rem;
  background:
    radial-gradient(circle at top left, rgba(174, 194, 181, 0.28), transparent 24%),
    radial-gradient(circle at right 20%, rgba(224, 206, 185, 0.24), transparent 22%),
    linear-gradient(180deg, #fcfbfa 0%, #f6f2eb 100%);
  color: #1e2821;
  font-family: 'Manrope', sans-serif;
}

.profile-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.profile-hero {
  margin-bottom: 2rem;
  padding: 1rem 0 2rem;
}

.hero-tag,
.section-kicker,
.portrait-kicker {
  display: inline-block;
  margin-bottom: 0.8rem;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #809084;
}

.hero-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.4rem, 4vw, 4rem);
  line-height: 1.08;
}

.hero-copy {
  max-width: 640px;
  margin: 1rem 0 0;
  color: #5d6b61;
  font-size: 1.02rem;
  line-height: 1.8;
}

.profile-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 2rem;
}

.profile-portrait-panel,
.profile-form-panel {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.portrait-card,
.readonly-card,
.profile-form-panel {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.78), rgba(248, 246, 242, 0.88));
  backdrop-filter: blur(24px);
  box-shadow: 0 40px 80px rgba(54, 66, 58, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.85);
  border-radius: 28px;
}

.portrait-card,
.readonly-card {
  padding: 1.5rem;
}

.profile-form-panel {
  padding: 2rem;
}

.portrait-circle {
  width: 220px;
  height: 220px;
  margin: 0 auto 1.25rem;
  border-radius: 50%;
  overflow: hidden;
  background: #d8e1db;
  box-shadow: 0 18px 36px rgba(46, 58, 50, 0.12);
}

.portrait-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portrait-copy h2,
.section-head h2,
.crop-copy h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.5rem;
  line-height: 1.35;
}

.portrait-copy p,
.readonly-title,
.section-note,
.crop-copy p,
.readonly-row span {
  color: #67756b;
}

.portrait-copy p:last-child {
  margin-bottom: 0;
}

.portrait-actions,
.crop-actions {
  display: flex;
  gap: 0.85rem;
  margin-top: 1.2rem;
}

.primary-btn,
.secondary-btn,
.save-btn {
  border: none;
  border-radius: 999px;
  padding: 0.95rem 1.35rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.primary-btn,
.save-btn {
  background: #24352c;
  color: #fff;
  box-shadow: 0 18px 36px rgba(36, 53, 44, 0.18);
}

.secondary-btn {
  background: rgba(36, 53, 44, 0.08);
  color: #24352c;
}

.primary-btn:hover:not(:disabled),
.secondary-btn:hover:not(:disabled),
.save-btn:hover:not(:disabled),
.preset-item:hover {
  transform: translateY(-4px);
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.8rem;
  margin-top: 1.2rem;
}

.preset-item {
  aspect-ratio: 1;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 20px;
  overflow: hidden;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.preset-item.is-active {
  border-color: #24352c;
  box-shadow: 0 20px 32px rgba(36, 53, 44, 0.14);
}

.preset-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.readonly-title {
  margin: 0 0 1rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.2rem;
}

.readonly-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.8rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
}

.readonly-row strong {
  color: #24352c;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
  margin-bottom: 1.2rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.2rem 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.field--full {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #738279;
}

.field-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid rgba(42, 54, 46, 0.14);
  padding: 0.9rem 0;
  background: transparent;
  color: #1e2821;
  font-size: 1rem;
  outline: none;
}

.field-input:focus {
  border-bottom-color: #24352c;
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.8rem;
}

.crop-modal {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 1.5rem;
  background: rgba(28, 36, 31, 0.42);
  backdrop-filter: blur(14px);
}

.crop-dialog {
  width: min(92vw, 640px);
  padding: 1.8rem;
  border-radius: 30px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.88), rgba(247, 243, 237, 0.92));
  box-shadow: 0 36px 72px rgba(28, 36, 31, 0.16);
}

.crop-stage {
  position: relative;
  width: 280px;
  height: 280px;
  margin: 1.4rem auto 0;
  overflow: hidden;
  border-radius: 30px;
  background: #d9e2dc;
  touch-action: none;
  cursor: grab;
}

.crop-stage:active {
  cursor: grabbing;
}

.crop-image {
  position: absolute;
  user-select: none;
  max-width: none;
}

.crop-mask {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at center, transparent 0 42%, rgba(17, 22, 19, 0.38) 42.8% 100%);
  pointer-events: none;
}

.crop-guide {
  position: absolute;
  inset: 22px;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.95), inset 0 0 0 1px rgba(255, 255, 255, 0.7);
  pointer-events: none;
}

.crop-controls {
  margin-top: 1.2rem;
}

.zoom-control {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.zoom-control span {
  min-width: 3rem;
  color: #607065;
}

.zoom-control input {
  width: 100%;
}

.hidden-input {
  display: none;
}

.error-banner,
.loading-state {
  margin-bottom: 1rem;
}

.error-banner {
  padding: 1rem 1.2rem;
  border-radius: 18px;
  background: rgba(155, 77, 77, 0.08);
  color: #914e4e;
}

.loading-state {
  text-align: center;
  padding: 4rem 0;
}

.spinner {
  width: 42px;
  height: 42px;
  margin: 0 auto 1rem;
  border-radius: 50%;
  border: 3px solid rgba(36, 53, 44, 0.12);
  border-top-color: #24352c;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .preset-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
